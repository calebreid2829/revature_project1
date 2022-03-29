package rawgRequests

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, sql}
import scala.io.StdIn.readLine
import scala.util.Random

import java.io.File

object Tabletest {
  var currentUserName = ""
  var currentUserRole = ""
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "C:\\hadoop")
    Logger.getLogger("org").setLevel(Level.ERROR)

    val conf = new SparkConf()

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
    var loop = true
    do {
      try {
        println("Please select an option:\n[1] Log In\n[2] New User\n[3] Shutdown")
        val line = readLine()

        line match {
          case "1" => {
            if (LogIn(spark)) {
              do {
                println("[1][2][3][4][5][6][change]")
                val line2 = readLine()
                line2 match {
                  case "1" => Query1(spark)
                  case "2" => Query2(spark)
                  case "3" => Query3(spark)
                  case "4" => Query4(spark)
                  case "5" => Query5(spark)
                  case "exit" => loop = false
                }
              } while (loop)
              loop = true
            }
            //if login good show rest
          }
          case "2" => AddUser(spark)
          case "3" => {
            println("Are you sure? [y][n]")
            val line2 = readLine()
            if (line2 == "y") loop = false
          }
        }
      }catch{case _: Exit =>}
    }while(loop)


    //"hdfs://localhost:9000/user/creid/tables/"
    //val file = new File("testing.json")
    //val js = ujson.read(file)

    /*spark.sql("DROP TABLE IF EXISTS testing123")
    val df = spark.sql("SELECT * FROM games").toDF()
    df.write.partitionBy("generation","platform")
      .mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/games")
    val df3 = spark.sql("SELECT * FROM genres").toDF()
    df3.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/genres")
    val df4 = spark.sql("SELECT * FROM genrelinks").toDF()
    df4.write.mode("overwrite")
      .partitionBy("genreid")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/genrelinks")
    val df5 = spark.sql("SELECT * FROM platforms").toDF()
    df5.write.mode("overwrite")
      .json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/platforms")
    val df2 = spark.read.json("hdfs://localhost:9000/user/hive/warehouse/gamesdb/games").toDF()
    df2.createOrReplaceTempView("games2")
    spark.sql("SELECT * FROM games2").show()
    spark.sql("SELECT COUNT(gameid) FROM games2").show()*/

    //spark.sql("CREATE TABLE users(userid Int,username String,password String)")
    //spark.sql("INSERT INTO users VALUES(1,\"user\",\"password\")")
    //spark.sql("INSERT INTO users VALUES(2,\"admin\",\"adminpassword\")")
    //spark.sql("SELECT * FROM users").show()
    //val users = spark.sql("SELECT * FROM users").toDF()
    //users.rdd.foreach(x=>println(x.getString(2)))
    //users.rdd.collect
    //users.rdd.toDebugString

  }

  def CreateTables(spark: SparkSession): Unit = {
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
  def ReadTables(spark: SparkSession): Unit={
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
    spark.sql("CREATE TABLE games(gameid Int, name String, release_date Date, metacritic Int, esrb String) " +
      "PARTITIONED BY (generation Int,platform String) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/games'")
    spark.sql("INSERT INTO games(SELECT gameid,name,release_date,metacritic,esrb,generation,platform FROM tmpgames)")
    spark.sql("DROP TABLE IF EXISTS genres")
    spark.sql("CREATE TABLE genres(genreid Int, name String) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/genres'")
    spark.sql("INSERT INTO genres(SELECT genreid,name FROM tmpge)")
    spark.sql("DROP TABLE IF EXISTS genrelinks")
    spark.sql("CREATE TABLE genrelinks(gameid Int,genreid Int) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/genrelinks'")
    spark.sql("INSERT INTO genrelinks(SELECT gameid,genreid FROM tmpgl)")
    spark.sql("DROP TABLE IF EXISTS platforms")
    spark.sql("CREATE TABLE platforms(platformid Int,parent_platformid Int,name String,games_count Int, " +
      "generation Int,start_date Int,end_date Int) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/platforms'")
    spark.sql("INSERT INTO platforms(SELECT platformid,parent_platformid,name,games_count," +
      "generation,start_date,end_date FROM tmpplatforms)")
    spark.sql("DROP TABLE IF EXISTS users")
    spark.sql("CREATE TABLE users(userid Int,username String, password String) LOCATION 'hdfs://localhost:9000/user/hive/warehouse/gamesdb/users'")
    spark.sql("INSERT INTO users(SELECT userid,username,password FROM tmpusers)")
    spark.sql("SELECT * FROM games").show()


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
      if(pass1==pass2){
        try {
          val user = spark.sql(s"SELECT * FROM users WHERE username='$name'").toDF()
          println("User: " + user.collect().array(0).getString(1)+"already exists")
          println("PLease enter a different username")
        }
        catch{
          case _: ClassCastException =>{
            val r = Random
            spark.sql(s"INSERT INTO users(userid,username,password) VALUES(${r.nextInt(1000)}'$name','$pass1')")
            loop = false
          }
        }

      }
      else println("Passwords do not match. Make sure they are the same")
    }while(loop)
  }
  def LogIn(spark:SparkSession): Boolean={
    val readmy = () => {val a = readLine();if(a.toLowerCase=="exit") throw Exit()else a}
    var loop = true
    do {
      println("[exit]")
      print("Username: ")
      val line1 = readmy()
      print("Password: ")
      val line2 = readmy()
      try {
        val user = spark.sql(s"SELECT * FROM users WHERE username='$line1' AND password='$line2'").toDF()
        println("Welcome: " + user.collect().array(0).getString(0))
        loop = false
      }
      catch{
        case _: ClassCastException =>{
          println("Username or password didn't match\nPlease try again")
        }
      }
    }while(loop)
    true
  }

  def Query1(spark: SparkSession): Unit = {
    spark.sql("SELECT platform,COUNT(gameid),ROUND(AVG(metacritic)) average FROM games " +
      "WHERE metacritic != -1 GROUP BY platform ORDER BY average DESC").show()
  }

  def Query2(spark: SparkSession): Unit = {
    val multiplats = spark.sql("SELECT gameid,COUNT(name) as ports FROM games GROUP BY gameid HAVING COUNT(name) > 1").toDF()
    multiplats.createOrReplaceTempView("multiplats")
    spark.sql("SELECT platform,COUNT(mp.gameid) multi_platforms FROM games g JOIN multiplats mp ON g.gameid=mp.gameid GROUP BY platform" +
      " ORDER BY multi_platforms DESC").show()
  }

  def Query3(spark: SparkSession): Unit = {
    val exclusives = spark.sql("SELECT gameid,COUNT(name) as exclusives FROM games GROUP BY gameid HAVING COUNT(name) =1").toDF()
    exclusives.createOrReplaceTempView("exclusives")
    spark.sql("SELECT platform,COUNT(mp.gameid) exclusives FROM games g JOIN exclusives mp ON g.gameid=mp.gameid GROUP BY platform" +
      " ORDER BY exclusives").show()
  }

  def Query4(spark: SparkSession): Unit = {
    spark.sql("SELECT platform,COUNT(gameid) as Released,Year(release_date) as year FROM games " +
      "JOIN platforms plat ON games.platform=plat.name " +
      "WHERE Year(release_date) BETWEEN start AND end "+
      "GROUP BY platform, year " +
      "ORDER BY year, platform").show()
  }

  def Query5(spark: SparkSession): Unit = {
    spark.sql("SELECT g.platform,ROUND(COUNT(g.name)/(Year(MAX(g.release_date))-Year(MIN(g.release_date)))) AS average_releases_per_year " +
      "FROM games g JOIN platforms plat ON g.platform=plat.name " +
      "WHERE Year(release_date) BETWEEN start AND end "+
      "GROUP BY platform " +
      "ORDER BY average_releases_per_year DESC").show()
  }

  case class Exit() extends Exception
}
