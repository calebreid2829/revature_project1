package rawgRequests
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.io.{File, FileReader, FileWriter, PrintWriter}
import collection.mutable.Buffer
import collection.mutable.ListBuffer
class Rawg(key:String) {
  System.setProperty("hadoop.home.dir","C:\\hadoop")
  //platforms: "https://api.rawg.io/api/platforms/lists/parents"
  //key: "87941bc8ee064f8f9a94665db098e462"
  //private val responses = Buffer.empty[ujson.Value]
  //private val data = ListBuffer.empty[Map[String, String]]
  private val genres = collection.mutable.Set.empty[(Int,String)]
  private val genrelinks = ListBuffer.empty[GenreLink]

  def GetPlatforms(): Unit ={
    val responses = GetResponses("https://api.rawg.io/api/platforms/lists/parents")
    val platforms = ListBuffer.empty[Platform]
    val parents = ListBuffer.empty[Parent_Platform]
    val tsti = (x:ujson.Value) => x.toString.toInt
    val ts = (x:ujson.Value) => x.toString
    responses.foreach(y => {
      val parent = new Parent_Platform(tsti(y.obj("id")),ts(y.obj("name")))
      parents.append(parent)
      val z = y.obj("platforms").arr
      z.foreach(x => {
        val platform = new Platform(tsti(x.obj("id")),tsti(y.obj("id")),ts(x.obj("name")),tsti(x.obj("games_count")))
        platforms.append(platform)
      })
    })
    WriteAll(platforms.toList,"platforms")
    WriteAll(parents.toList,"parent_platforms")

  }
  /*def GetGames(): Unit= {
    val gprint = new PrintWriter("games.json")
    val geprint = new PrintWriter("genreLinks.json")
    val tprint = new PrintWriter("tagLinks.json")
    val plprint = new PrintWriter("platformLinks.json")

    val genreSet = collection.mutable.Set.empty[(Int, String)]
    val tagSet = collection.mutable.Set.empty[(Int,String)]

    try {
      gprint.print("[")
      geprint.print("[")
      tprint.print("[")
      plprint.print("[")
      val tsti = (x: ujson.Value) => try {
        if (x != null) x.toString.toInt else -1
      } catch {
        case _: Any => -1
      }
      val ts = (x: ujson.Value) => try {
        if (x != "null") x.toString else "null"
      } catch {
        case _: Any => "null"
      }
      var done = false
      var page = 1
      do {
        println("page: " + page + "...")
        //val r = Responses("https://api.rawg.io/api/games", Map("key" -> key, "page_size" -> "40","page"->page.toString, "parent_platforms" -> "2,3,7,9,11,12,13"))
        val r = Responses("https://api.rawg.io/api/games", Map("key"->key,"page_size"->"40","platforms"->"15,17,80,9,13,11,105,106","page"->page.toString))
        val responses = ujson.read(r.text).obj("results")
        if (ujson.read(r.text).obj("next").toString != "null") page += 1
        else done = true
        responses.arr.foreach(y => {
          var esrb = ""
          if (y.obj("esrb_rating").toString == "null") esrb = "null"
          else esrb = ts(y.obj("esrb_rating").obj("name"))
          val game = new Game(tsti(y.obj("id")), ts(y.obj("name")), ts(y.obj("released")), tsti(y.obj("metacritic")), tsti(y.obj("playtime"))
            , ts(y.obj("updated")), esrb)
          if (done && y.obj("id") == responses.arr.last.obj("id")) {
            Write(game, gprint, false)
          }
          else Write(game, gprint, true)
          try {
            y.obj("genres").arr.foreach(z => {
              val genrelink = new GenreLink(tsti(y.obj("id")), tsti(z.obj("id")))
              try{genreSet.add(tsti(z.obj("id")),ts(z.obj("name")))}catch{case _:Any=>}
              if (done && y.obj("id") == responses.arr.last.obj("id") &&
                y.obj("genres").arr.last.obj("id") == z.obj("id")) Write(genrelink, geprint, false)
              else Write(genrelink, geprint, true)
            })
          }
          catch{
            case _: Any => println("No genres. Skipping")
          }
          try {
            y.obj("tags").arr.foreach(z => {
              val tagLink = new TagLink(tsti(y.obj("id")), tsti(z.obj("id")))
              try{tagSet.add(tsti(z.obj("id")),ts(z.obj("name")))}catch{case _:Any=>}
              if (done && y.obj("id") == responses.arr.last.obj("id") &&
                y.obj("tags").arr.last.obj("id") == z.obj("id")) Write(tagLink, tprint, false)
              else Write(tagLink, tprint, true)
            })
          }
          catch{
            case _: Any => println("No tags. Skipping")
          }
          y.obj("platforms").arr.foreach(z => {
            val platformLink = new PlatformLink(tsti(y.obj("id")), tsti(z.obj("platform").obj("id")))
            if (done && y.obj("id") == responses.arr.last.obj("id") &&
              y.obj("platforms").arr.last.obj("platform").obj("id") == z.obj("platform").obj("id")) Write(platformLink, plprint, false)
            else Write(platformLink, plprint, true)
          })
        })
      } while (!done)
    }
    catch {
      //case _: Any => println("There was an error, saving requests so far")
      case _: NumberFormatException =>
    }
    finally {

      try{WriteAll(genreSet.toList,"hdfs://localhost:9000/user/hive/warehouse/tables/genres")}
      catch{case _: Any => WriteAll(genreSet.toList,"genres")}
      WriteAll(tagSet.toList,"tags")

      gprint.print("]");
      gprint.close()
      geprint.print("]");
      geprint.close()
      tprint.print("]");
      tprint.close()
      plprint.print("]");
      plprint.close()
    }
  }
*/
  def GetGames(): Seq[Game]={

    val tsti=(x:ujson.Value)=>try{if(x!=null)x.toString.toInt else-1}catch{case _:Any=> -1}
    val ts=(x:ujson.Value)=>try{if(x!="null")x.toString.replace("\"","") else "null"}catch{case _:Any=>"null"}

    var page = 1
    var done = false
    val games = new ListBuffer[Game]
    do {
      println("page: "+page+"...")
      val r = Responses("https://api.rawg.io/api/games", Map("key" -> key, "page_size" -> "40",
        "platforms" -> "15,17,80,105,106,24", "page" -> page.toString))
      val responses = ujson.read(r.text).obj("results")

      if (ujson.read(r.text).obj("next").toString != "null") page += 1
      else done = true
      responses.arr.foreach(y=>{
        var esrb = ""
        if (y.obj("esrb_rating").toString == "null") esrb = "null"
        else esrb = ts(y.obj("esrb_rating").obj("name"))

        y.obj("platforms").arr.foreach(x=>{
          val platform = ts(x.obj("platform").obj("name"))
          val generation = platform match{
            case "PlayStation 2"|"Dreamcast"|"GameCube"|"Game Boy Advance"|"Xbox"|"PSP" => 6
            case _ => -1
            //case "Playstation"|"Nintendo 64"|"Game Boy Advance"|"SEGA Saturn" =>5
            //case "Playstation 3"|"Wii"|"xbox 360"|"Nintendo DS"|"PS Vita" =>7
          }
          if(generation != -1) {
            val gm = Game(tsti(y.obj("id")), ts(y.obj("name")), ts(y.obj("released")), tsti(y.obj("metacritic")), esrb, generation, platform)
            games.append(gm)
          }
        })
        y.obj("genres").arr.foreach(x=>{
          genrelinks.append(GenreLink(tsti(y.obj("id")),tsti(x.obj("id"))))
          try{genres.add(tsti(x.obj("id")),ts(x.obj("name")))}catch{case _: Any=>}
        })

      })

    }while(!done)
    println("Done!")
      val seqgames = games.toSeq
    seqgames
  }
  def GetGenres(): Seq[(Int,String)]={
    genres.toSeq
  }
  def GetGenreLinks(): Seq[GenreLink] ={
    genrelinks
  }

  private def GetResponses(url:String): Array[ujson.Value] ={
    val responses = Buffer.empty[ujson.Value]
    var page = 1
    var done = false
    do {
      println(page+"...")
      val r = requests.get(url, params = Map("key" -> key, "page" -> page.toString,"page_size"->"40"))
      if (ujson.read(r.text).obj("next").toString!= "null") page += 1
      else done = true
      val results = ujson.read(r.text).obj("results")
      responses.appendAll(results.arr)
    }while(!done)
    print("Data Received!\nCompiling...")
    responses.toArray[ujson.Value]

  }
  private def Responses(url:String,param:Map[String,String]): requests.Response={
    val r = requests.get(url,params = param)
    //ujson.read(r.text).obj("results")
    r
  }
  private def tmpGetResponses(url:String): Array[ujson.Value]={
    val responses = Buffer.empty[ujson.Value]


    val gprint = new PrintWriter("games.json")
    val geprint = new PrintWriter("genreLinks.json")
    val tprint = new PrintWriter("tagLinks.json")
    gprint.print("[")
    geprint.print("[")
    tprint.print("[")
    val games = ListBuffer.empty[Game]
    val genreLinks = ListBuffer.empty[GenreLink]
    val tagLinks = ListBuffer.empty[TagLink]
    val tsti = (x:ujson.Value) => try{if(x!= null)x.toString.toInt else -1}catch{case _: Any => -1}
    val ts = (x:ujson.Value) => try{if(x!= "null")x.toString else "null"}catch{case _: Any =>"null"}

    var page = 1
    var done = false
    do {
      println(page + "...")
      val r = requests.get(url, params = Map("key" -> key, "page" -> page.toString, "page_size" -> "40"))
      if (ujson.read(r.text).obj("next").toString != "null") page += 1
      else done = true
      val results = ujson.read(r.text).obj("results")
      responses.appendAll(results.arr)
      if(page%10 == 0){
        games.foreach(x=>{
          gprint.print("{")

          gprint.print("}")
        })
      }
    }while(!done)
    print("Data Received!\nCompiling...")
    responses.toArray[ujson.Value]
  }
  private def WriteAll(list: List[Data],file_name:String): File = {
    val printer = new PrintWriter(file_name + ".json")
    val keyVal = (x:String,y:Any,z:Boolean) =>{ printer.print("\""+x+"\":"+y); if(z)printer.print(",")}
    printer.print("[")
    list.foreach(x =>{
      printer.print("{")
      x.Write(keyVal(_,_,_))
      printer.print("}")
      if(x.toString != list.last.toString) printer.print(",")
    })
    printer.print("]")
    printer.close()
    val file = new File(file_name + ".json")
    file
  }
  private def WriteAll(list: List[(Int,String)],file_name:String): Unit={
    val printer = new PrintWriter(file_name + ".json")
    val keyVal = (x:String,y:Any,z:Boolean) =>{ printer.print("\""+x+"\":"+y); if(z)printer.print(",")}
    printer.print("[")
    list.foreach(x =>{
      printer.print("{")
      keyVal("id",x._1,true)
      keyVal("name",x._2,false)
      printer.print("}")
      if(x.toString != list.last.toString) printer.print(",")
    })
    printer.print("]")
    printer.close()
  }
  private def Write(item:Data,printer:PrintWriter,comma:Boolean): Unit ={
    val keyVal = (x:String,y:Any,z:Boolean) =>{ printer.write("\""+x+"\":"+y); if(z)printer.write(",")}
      printer.print("{")
      item.Write(keyVal(_,_,_))
    printer.print("}")
    if(comma)printer.write(",")
  }

}
case class Game(id:Int,name:String,release_date:String,metacritic: Int,esrb:String,generation:Int,platform: String)
class Data(){
  def Write(keyVal: (String,Any,Boolean)=>Unit): Unit={}
}
class Platform(val id:Int,val parent_platform_id:Int,val name:String,val games_count:Int) extends Data{
  override def Write(keyVal: (String,Any,Boolean)=>Unit): Unit = {
    keyVal("platformid",id,true)
    keyVal("parent_platformid",parent_platform_id,true)
    keyVal("name",name,true)
    keyVal("games_count",games_count,false)
  }
}
class PlatformLink(val gameid:Int,val platformid:Int) extends Data{
  override def Write(keyVal: (String,Any,Boolean)=>Unit): Unit={
    keyVal("gameid",gameid,true)
    keyVal("platformid",platformid,false)
  }
}
class Parent_Platform(val id:Int,val name:String) extends Data{
  override def Write(keyVal: (String,Any,Boolean)=>Unit): Unit={
    keyVal("parent_platformid",id,true)
    keyVal("name",name,false)
  }
}
/*
class Game(val id:Int,val name:String,val release_date:String,val metacritic: Int,val playtime: Int,val last_updated:String,val esrb: String) extends Data{

  override def Write(keyVal: (String,Any,Boolean)=>Unit):Unit={
    keyVal("gameid",id,true)
    keyVal("name",name,true)
    keyVal("release_date",release_date,true)
    keyVal("metacritic",metacritic,true)
    keyVal("playtime",playtime,true)
    keyVal("last_updated",last_updated,true)
    keyVal("esrb",esrb,false)
  }

}
class Game2(val id:Int,val name:String,val release_date:String,val metacritic: Int,val playtime: Int,val last_updated:String,val esrb: String, val platform: String) extends Data{

  override def Write(keyVal: (String,Any,Boolean)=>Unit):Unit={
    keyVal("gameid",id,true)
    keyVal("name",name,true)
    keyVal("release_date",release_date,true)
    keyVal("metacritic",metacritic,true)
    keyVal("playtime",playtime,true)
    keyVal("last_updated",last_updated,true)
    keyVal("esrb",esrb,true)
    keyVal("platform",platform,false)
  }

}
*/
class Genre(val id:Int,val name:String) extends Data{
  override def Write(keyVal: (String,Any,Boolean)=>Unit):Unit= {
    keyVal("genreid",id,true)
    keyVal("name",name,false)
  }
}
case class GenreLink(val gameid:Int,val genreid:Int){

}
class Tag(val id:Int,val name:String) extends Data{
  override def Write(keyVal: (String,Any,Boolean)=>Unit): Unit = {
    keyVal("id",id,true)
    keyVal("name",name,false)
  }
}
class TagLink(val gameid:Int,val tagid:Int) extends Data{
  override def Write(keyVal: (String,Any,Boolean)=>Unit):Unit={
    keyVal("gameid",gameid,true)
    keyVal("tagid",tagid,false)
  }
}
