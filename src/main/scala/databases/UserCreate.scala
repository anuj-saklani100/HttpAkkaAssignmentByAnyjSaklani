package databases
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

import java.sql.{Connection, DriverManager, ResultSet}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
case class USER1(id:Int,name:String,startTime:String,create_at:String)
//implicit val defaultTimeout=Timeout(2 seconds )
trait Inmagic extends DefaultJsonProtocol{
  implicit val doMagic=jsonFormat4(USER1)
}
object UserCreate extends Inmagic with SprayJsonSupport{
def main(args:Array[String]):Unit={
  val url = "jdbc:mysql://localhost:3306/myNewDatabase"
  val username = "root"
  val password = "Tellius@12345"

  val caller=new UserCreate(url, username, password)
 // here we have to write our protocols
 import akka.http.scaladsl.server.Directives._
  val reqHandler1=
    path("api"){
      post{
        entity(as[USER1]){
          (a)=>{
            val res3=caller.addUser(a)
            complete(res3)
          }
        }
      }~
        patch{
          entity(as[USER1]){
            (a)=>{
              parameter('id.as[Int]){
                (x)=>{
                  val res4 = caller.updateById(x, a)
                  complete(res4)
                }
              }
            }
          }
        }~
      parameter('id.as[Int]){
        (a)=>get{
          val res1=caller.getUserById(a)
          complete(res1)
        }
      }~
      get{
        val res2=caller.getAllUsers()
        complete(res2)
      }
    }

  implicit val system = ActorSystem("tests")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  Http().bindAndHandle(reqHandler1, "localhost", 9091)


}

  class UserCreate(url:String,username:String,password:String){
    // method to establish connection
    private def getConnection():Connection={
      DriverManager.getConnection(url,username,password)
    }

    // method to parse the result set

    private def parseResultSet(resultSet: ResultSet): List[USER1] = {
      var users: List[USER1] = List.empty[USER1]
      while (resultSet.next()) {
        val id = resultSet.getInt("id")
        val name = resultSet.getString("name")
        val startTime = resultSet.getString("startTime")
        val create_at = resultSet.getString("create_at")
        val user = USER1(id, name, startTime, create_at)
        users = user :: users
      }
      users.reverse
    }


    // Methods for CRUD operations

    //to get all users
    def getAllUsers():List[USER1]={
      val connection=getConnection()
      val statement= connection.createStatement()
      val query="SELECT * FROM USERS1"
      val resultSet= statement.executeQuery(query)
      val fetched_users= parseResultSet(resultSet)
      resultSet.close()
      statement.close()
      connection.close()

      fetched_users
    }

    // to get user by id
    def getUserById(ids:Int):Option[USER1]={
      val connection=getConnection()
      val statement= connection.createStatement()
      val query=s"SELECT * FROM USERS1 WHERE id=$ids"
      val resultSet=statement.executeQuery(query)
      val fetched_users=
      if(resultSet.next()){
        val id= resultSet.getInt("id")
        val name=resultSet.getString("name")
        val startTime = resultSet.getString("startTime")
        val create_at = resultSet.getString("create_at")

       Some(USER1(id, name, startTime, create_at))
      }else{
        None
      }
      resultSet.close()
      statement.close()
      connection.close()
      fetched_users
    }

    // update the information via id
    def updateById(ids:Int,users:USER1):Option[USER1]={
      val currentDateTime = LocalDateTime.now()
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val currentDateTimeString = currentDateTime.format(formatter)
      val connection= getConnection()
      val statement= connection.createStatement()
      val query=s"UPDATE USERS1 SET name='${users.name}',startTime='${users.startTime}',create_at='$currentDateTimeString' WHERE id=$ids"
      val rowUpdation=statement.executeUpdate(query)
      println(s"The query is: $query")
      statement.close()
      connection.close()
      None
    }


    // add user in the database
    def addUser(users:USER1):Option[USER1]={
      val currentDateTime = LocalDateTime.now()
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val currentDateTimeString = currentDateTime.format(formatter)
      val connection = getConnection()
      val statement = connection.createStatement()
      val query= s"INSERT INTO USERS1 (id, name, startTime,create_at) VALUES (?,?,?,?)"
      val preparedStatement=connection.prepareStatement(query)
      preparedStatement.setInt(1, 4)
      preparedStatement.setString(2, users.name)
      preparedStatement.setString(3, currentDateTimeString)
      preparedStatement.setString(4, currentDateTimeString)
      val resultSet=preparedStatement.executeUpdate()

      if(resultSet>0){
        statement.close()
        connection.close()
        Some(users)
      }else{
        statement.close()
        connection.close()
        None
      }
    }


  }
}
