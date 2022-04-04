package rawgRequests

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, sql}
import rawgRequests.states.states
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.functions.{col, when}

import scala.io.StdIn.readLine
import scala.util.Random
import java.io.File
import scala.language.postfixOps

object states extends Enumeration{
  type states = Value

  val main = Value(0)
  val logIn = Value(1)
  val logOut = Value(3)
  val page = Value(4)
  val newUser = Value(5)
  val changeInfo = Value(6)
  val shutdown = Value(7)
  val done = Value(8)

}

object Tabletest {
  var currentUserName = ""
  var currentUserRole = ""
  var currentUserPassword = ""
  var state = states.main

  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "C:\\hadoop")
    val conf = new SparkConf()
    Logger.getLogger("org").setLevel(Level.OFF)
    val spark = SparkSession
      .builder()
      .appName("api test")
      .config(conf)
      .config("spark.master", "local")
      .config("spark.sql.warehouse.dir","hdfs://localhost:9000/user/hive/warehouse/gamesdb")
      .enableHiveSupport()
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")
    spark.conf.set("hive.exec.dynamic.partition.mode", "nonstrict")
    import spark.implicits._
    val a= Seq(("user","password","user"),("admin","adminpassword","admin"),("billy","billypassword","user")).toDF("username","password","role")
    //val users = spark.sparkContext.parallelize(a)
    //val us = spark.createDataFrame(users).toDF("username","password","role")
    a.createOrReplaceTempView("users2")
    spark.sql("DROP TABLE IF EXISTS users")
    spark.sql("CREATE TABLE users(username String,password String,role String)")
    spark.sql("INSERT INTO users (SELECT username,password,role FROM users2)")
    //val users = spark.read.json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/users").toDF()
    //users.createOrReplaceTempView("users")
    var loop = true
    //UserTable(spark)
    do{
      state match{
        case states.main => MainMenu()
        case states.logIn => try{state=LogIn(spark:SparkSession)}catch{case _: Exit=>state=states.main}
        case states.logOut => Logout()
        case states.newUser => AddUser(spark:SparkSession)
        //case states.changeInfo =>
        case states.page => UserPage(spark:SparkSession)
        case states.shutdown => {
          println("Are you sure?\n[y] [n]")
          if(readLine().toLowerCase() =="y"){loop = false;state=states.done}
        }
        case _ =>
      }
    }while(loop)

  }
  def MainMenu(): Unit={
    println("Please select an option:\n[1] Log In\n[2] New User\n[3] Shutdown")
    val line = readLine()
    line match{
      case "1" => state = states.logIn
      case "2" => state = states.newUser
      case "3" => state = states.shutdown
    }
  }
  def CreateTables(spark:SparkSession): Unit = {
    val sc = spark.sparkContext
    val rawg = new Rawg("87941bc8ee064f8f9a94665db098e462")
    val games = sc.parallelize(rawg.GetGames())
    val gl = sc.parallelize(rawg.GetGenreLinks())
    val ge = sc.parallelize(rawg.GetGenres())
    val tmpgames = spark.createDataFrame(games).toDF("gameid", "name", "release_date", "metacritic", "esrb", "generation", "platform")
    val tmpgl = spark.createDataFrame(gl).toDF("gameid", "genreid")
    val tmpge = spark.createDataFrame(ge).toDF("genreid", "name")

    /*tmpgames.write.partitionBy("generation","platform")
      .mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/games")
    tmpgl.write.partitionBy("genreid")
      .mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/genrelinks")
    tmpge.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/genres")*/

    tmpgames.createOrReplaceTempView("new")
    tmpgl.createOrReplaceTempView("tmpgl")
    tmpge.createOrReplaceTempView("tmpge")
    spark.sql("DROP TABLE IF EXISTS games")
    spark.sql("CREATE EXTERNAL TABLE games(gameid Int, name String, release_date Date, metacritic Int, esrb String) " +
      "PARTITIONED BY (generation Int,platform String) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/games'")
    spark.sql("INSERT INTO games(SELECT gameid,name,release_date,metacritic,esrb,generation,platform FROM new)")
    spark.sql("DROP TABLE IF EXISTS genres")
    spark.sql("CREATE EXTERNAL TABLE genres(genreid Int, name String) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/genres'")
    spark.sql("INSERT INTO genres(SELECT genreid,name FROM tmpge)")
    spark.sql("DROP TABLE IF EXISTS genrelinks")
    spark.sql("CREATE EXTERNAL TABLE genrelinks(gameid Int,genreid Int) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/genrelinks'")
    spark.sql("INSERT INTO genrelinks(SELECT gameid,genreid FROM tmpgl)")
    spark.sql("SELECT platform, COUNT(name) FROM games WHERE generation=6 GROUP BY platform").show()
    spark.sql("SELECT * FROM genres").show()
    spark.sql("SELECT COUNT(gameid) FROM genrelinks").show()
  }
  def ReadTables(spark:SparkSession): Unit={
    val games = spark.read.json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/games").toDF()
    val genres = spark.read.json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/genres").toDF()
    val genrelinks = spark.read.json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/genrelinks").toDF()
    val platforms = spark.read.json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/platforms").toDF()
    val users = spark.read.json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/users").toDF()
    games.createOrReplaceTempView("tmpgames")
    genres.createOrReplaceTempView("tmpge")
    genrelinks.createOrReplaceTempView("tmpgl")
    platforms.createOrReplaceTempView("tmpplatforms")
    users.createOrReplaceTempView("tmpusers")

    spark.sql("DROP TABLE IF EXISTS games")
    spark.sql("CREATE EXTERNAL TABLE games(gameid Int, name String, release_date Date, metacritic Int, esrb String) " +
      "PARTITIONED BY (generation Int,platform String) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/games'")
    spark.sql("INSERT INTO games(SELECT gameid,name,release_date,metacritic,esrb,generation,platform FROM tmpgames)")
    spark.sql("DROP TABLE IF EXISTS genres")
    spark.sql("CREATE EXTERNAL TABLE genres(genreid Int, name String) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/genres'")
    spark.sql("INSERT INTO genres(SELECT genreid,name FROM tmpge)")
    spark.sql("DROP TABLE IF EXISTS genrelinks")
    spark.sql("CREATE EXTERNAL TABLE genrelinks(gameid Int,genreid Int) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/genrelinks'")
    spark.sql("INSERT INTO genrelinks(SELECT gameid,genreid FROM tmpgl)")
    spark.sql("DROP TABLE IF EXISTS platforms")
    spark.sql("CREATE EXTERNAL TABLE platforms(platformid Int,parent_platformid Int,name String,games_count Int, " +
      "generation Int,start_date Int,end_date Int) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/platforms'")
    spark.sql("INSERT INTO platforms(SELECT platformid,parent_platformid,name,games_count," +
      "generation,start_date,end_date FROM tmpplatforms)")
    spark.sql("DROP TABLE IF EXISTS users")
    spark.sql("CREATE EXTERNAL TABLE users(userid Int,username String, password String) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/users'")
    spark.sql("INSERT INTO users(SELECT userid,username,password FROM tmpusers)")
    //spark.sql("SELECT * FROM games").show()


  }
  def AddUser(spark:SparkSession): Unit={
    var loop = true
    do{
      print("Enter your desired username: ")
      val name = readLine()
      print("Enter your desired password: ")
      val pass1 = readLine()
      print("Confirm password: ")
      val pass2 = readLine()
      if(pass1.equals(pass2)){
          import spark.implicits._
          //val users = spark.read.json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/users")
           // .toDF("username","password","role")
          val users = spark.sql("SELECT * FROM users").toDF()
          if(currentUserRole=="admin") {
            print("Enter role: ")
            val role = readLine()
            val user = Seq(name,pass1,role).toDF("username","password","role")
            user.createOrReplaceTempView("users2")
            spark.sql("INSERT INTO users( SELECT username,password,role FROM users2)")
          }
          else {
            val user = Seq(name,pass1,"user").toDF("username","password","role")
            user.createOrReplaceTempView("users2")
            spark.sql("INSERT INTO users( SELECT username,password,role FROM users2)")
          }
        loop = false
      }
      else println("Passwords do not match. Make sure they are the same")
    }while(loop)
    if(currentUserName!="") state=states.page
    else state=states.main
  }
  def LogIn(spark:SparkSession): states={
    val readmy = () => {val a = readLine();if(a.toLowerCase=="exit") throw Exit()else a}
    var loop = true
    do {
      println("[exit]")
      print("Username: ")
      val line1 = readmy()
      print("Password: ")
      val line2 = readmy()
      try {
        val user = spark.sql(s"SELECT * FROM users WHERE username='$line1' AND password='$line2'").toDF("username","password","role")
        currentUserName = user.collect().array(0).getString(0)
        currentUserRole = user.collect().array(0).getString(2)
        currentUserPassword = user.collect().array(0).getString(1)
        println(s"Welcome: $currentUserName")
        loop = false
      }
      catch{
        case _: ClassCastException =>{
          println("Username or password didn't match\nPlease try again")
        }
      }
    }while(loop)
    states.page
  }
  def Logout(): Unit={
    println("Are you sure you want to logout?\n[y] [n]")
    if(readLine().toLowerCase=="y"){currentUserName="";currentUserRole="";state=states.main}
    else state=states.page
  }
  def UserPage(spark:SparkSession): Unit ={
    println("[0] Change user info\n[1] Average Review Scores Per System\n[2] Number of multiplatform games per system\n" +
      "[3] Number of exclusives per system\n[4] Games released per system per year\n" +
      "[5] Average releases per year per system\n[6] Highest rated games per genre\n[logout]")
    if(currentUserRole=="admin")println("[pull]Pull data from API")
    val line2 = readLine()
    line2 match {
      case "add" => if(currentUserRole=="admin")AddUser(spark:SparkSession)
      case "0" => ChangeInfo(spark)
      case "1" => Query1(spark)
      case "2" => Query2(spark)
      case "3" => Query3(spark)
      case "4" => Query4(spark)
      case "5" => Query5(spark)
      case "6" => Query6(spark)
      case "logout" => state=states.logOut
      case "pull" => if(currentUserRole=="admin") CreateTables(spark)
      case _ =>
    }
  }
  def ChangeInfo(spark:SparkSession): Unit={
    var newName =currentUserName
    var newPass =currentUserPassword
    var loop = true
    do{
      println(s"Username: $newName\nPassword: $newPass")
      println("What do you want to change?\n[1] Username\n[2] Password\n[3] Save and Submit")
      readLine() match{
        case "1" =>
          print("Enter new Username: ");newName=readLine()
        case "2" =>
          print("Enter new Password: ");newPass=readLine()
        case "3" =>
          spark.sql(s"INSERT INTO users Values('$newName','$newPass','$currentUserRole')")
          currentUserPassword = newPass
          currentUserName = newName
            loop = false
      }
    }while(loop)
  }
  def Query1(spark:SparkSession): Unit = {
    val q =spark.sql("SELECT platform,COUNT(gameid),ROUND(AVG(metacritic)) average FROM games " +
      "WHERE metacritic != -1 GROUP BY platform ORDER BY average DESC")
    q.show()
    q.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/results/1")
  }
  def Query2(spark:SparkSession): Unit = {
    val multiplats = spark.sql("SELECT gameid,COUNT(name) as ports FROM games GROUP BY gameid HAVING COUNT(name) > 1").toDF()
    multiplats.createOrReplaceTempView("multiplats")
    val q=spark.sql("SELECT platform,COUNT(mp.gameid) multi_platforms FROM games g JOIN multiplats mp ON g.gameid=mp.gameid GROUP BY platform" +
      " ORDER BY multi_platforms DESC")
    q.show()
    q.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/results/2")
  }
  def Query3(spark:SparkSession): Unit = {
    val exclusives = spark.sql("SELECT gameid,COUNT(name) as exclusives FROM games GROUP BY gameid HAVING COUNT(name) =1").toDF()
    exclusives.createOrReplaceTempView("exclusives")
    val q=spark.sql("SELECT platform,COUNT(mp.gameid) exclusives FROM games g JOIN exclusives mp ON g.gameid=mp.gameid GROUP BY platform" +
      " ORDER BY exclusives")
    q.show()
    q.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/results/3")
  }
  def Query4(spark:SparkSession): Unit = {
    val q =spark.sql("SELECT platform,COUNT(gameid) as Released,Year(release_date) as year FROM games " +
      "JOIN platforms plat ON games.platform=plat.name " +
      "WHERE Year(release_date) BETWEEN start AND end "+
      "GROUP BY platform, year " +
      "ORDER BY year, platform")
    q.show(40)
    q.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/results/4")
  }
  def Query5(spark:SparkSession): Unit = {
    val q=spark.sql("SELECT g.platform,ROUND(COUNT(g.name)/(Year(MAX(g.release_date))-Year(MIN(g.release_date)))) AS average_releases_per_year " +
      "FROM games g JOIN platforms plat ON g.platform=plat.name " +
      "WHERE Year(release_date) BETWEEN start AND end "+
      "GROUP BY platform " +
      "ORDER BY average_releases_per_year DESC")
    q.show()
    q.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/results/5")
  }
  def Query6(spark:SparkSession): Unit={
    val by_genres = spark.sql("SELECT ge.name as genre,g.name as game,g.metacritic as metacritic FROM games g\nJOIN genrelinks gl ON g.gameid=gl.gameid\nJOIN genres ge ON gl.genreid=ge.genreid\nWHERE metacritic != -1 AND ge.name!=\"Indie\" AND ge.name!=\"Family\" AND ge.name!=\"Board Games\" AND ge.name!=\"Educational\" AND generation=6\nGROUP BY genre,game,metacritic\nORDER BY g.metacritic DESC").toDF()
    by_genres.createOrReplaceTempView("by_genres")
    val q=spark.sql("SELECT genre,first(game) as game FROM by_genres GROUP BY genre ORDER BY game")
    q.show(40)
    q.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/results/6")
  }
  case class Exit() extends Exception
}


