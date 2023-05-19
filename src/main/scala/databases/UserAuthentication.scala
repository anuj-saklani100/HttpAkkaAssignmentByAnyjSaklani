package databases
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

import java.sql.{Connection, DriverManager, ResultSet}
import scala.concurrent.duration._

case class TelliusUser(id: Int, name: String, startTime: String)

trait robot extends DefaultJsonProtocol {
  implicit val change = jsonFormat3(TelliusUser)
}

object UserAuthentication extends SprayJsonSupport with robot{
  def main(args: Array[String]): Unit = {
    val url = "jdbc:mysql://localhost:3306/authenticUser"
    val username = "root"
    val password = "Tellius@12345"

    val userDao = new UserAuthentication(url, username, password)

    // Use userDao to perform CRUD operations
    import akka.http.scaladsl.server.Directives._
    import spray.json._
    val requestHandler: Route =
      pathPrefix("api" / "user") {
        delete{
          parameter('id.as[Int]){
            (a)=>{
              val res=userDao.userDelete(a)
              complete(res.map(_ =>StatusCodes.OK))
            }
          }
        }~
      get{
        (path(Segment) | parameter("username")){
          (a)=>{
            val res=userDao.getAllUsers(a)
            if(res.isEmpty)complete(StatusCodes.NoContent)
            else complete(res)
          }
        }
      }
      }



    implicit val system=ActorSystem("tests")
    implicit val materializer=ActorMaterializer()
    import system.dispatcher
    Http().bindAndHandle(requestHandler,"localhost",7001)
  }

  class UserAuthentication(url: String, username: String, password: String) {
    private def getConnection(): Connection =
      DriverManager.getConnection(url, username, password)

    private def parseResultSet(resultSet: ResultSet): List[TelliusUser] = {
      var users: List[TelliusUser] = List.empty[TelliusUser]
      while (resultSet.next()) {
        val id = resultSet.getInt("id")
        val name = resultSet.getString("name")
        val startTime = resultSet.getString("startTime")
        val user = TelliusUser(id, name, startTime)
        users = user :: users
      }
      users.reverse
    }
// both are mutable because we are updating it so val we can't take here
var existingUsers=List ("Abhijaat","Anuj","Prateek","Maithri","Vamsi") // all these are the existing users that can access the list of all users
// map to track the id corresponding to the user
    var hash:Map[Int,String]=Map(
      (1->"Abhijaat"),
      (2->"Anuj"),
      (3->"Prateek"),
      (4->"Maithri"),
      (5->"Vamsi")
    )
    // Implement CRUD operations here
    def getAllUsers(username:String): List[TelliusUser] = {
      val connection = getConnection()
      val statement = connection.createStatement()
      if(existingUsers.contains(username)){
        val query = "SELECT * FROM authUser"
        val resultSet = statement.executeQuery(query)
        val users = parseResultSet(resultSet)
        resultSet.close()
        statement.close()
        connection.close()
        users
      }
      else{
        List.empty[TelliusUser]
      }

    }


    // lets confirm out code and database more deeply
    // lets delete some existing user and check after deleting they are no more existing user
    // and cant access the all users list

    def userDelete(ids:Int):Option[TelliusUser]={
      val connection=getConnection()
      val statement=connection.createStatement()
      val query=s"DELETE FROM authUser WHERE id=$ids"
      val q2=s"SELECT name FROM authUser WHERE id=$ids"

      val resultSet=statement.executeUpdate(query)
       val name=hash.get(ids)
      name match{
        case None=>
        case Some(value)=> {
          existingUsers=existingUsers.filterNot(_==value)
          hash= hash - ids
        }
      }


      statement.close()
      connection.close()
      None
    }




  }
}



/*
------------------------------------------------- OUTPUT ----------------------------------------------

---------> right now my database is empty

➜  newhttppractice git:(main) ✗ http get "localhost:7001/api/user?username=Anuj"
HTTP/1.1 204 No Content
Date: Fri, 19 May 2023 07:28:58 GMT
Server: akka-http/10.1.7


------->  deleted the suer by id 1, that is Abhijaat

➜  newhttppractice git:(main) ✗ http delete "localhost:7001/api/user?id=1"
HTTP/1.1 200 OK
Content-Length: 0
Date: Fri, 19 May 2023 07:40:08 GMT
Server: akka-http/10.1.7


------> Anuj is existing member and can access the list of all users
➜  newhttppractice git:(main) ✗ http get "localhost:7001/api/user?username=Anuj"
HTTP/1.1 200 OK
Content-Length: 200
Content-Type: application/json
Date: Fri, 19 May 2023 07:40:32 GMT
Server: akka-http/10.1.7

[
    {
        "id": 2,
        "name": "Anuj",
        "startTime": "2023-05-21"
    },
    {
        "id": 3,
        "name": "Prateek",
        "startTime": "2023-05-22"
    },
    {
        "id": 4,
        "name": "Maithri",
        "startTime": "2023-05-22"
    },
    {
        "id": 5,
        "name": "Vamsi",
        "startTime": "2023-05-22"
    }
]

-------> Abhijat is no more existing member so now he can't access the user list

➜  newhttppractice git:(main) ✗  http get "localhost:7001/api/user?username=Abhijaat"

HTTP/1.1 204 No Content
Date: Fri, 19 May 2023 07:41:39 GMT
Server: akka-http/10.1.7



----> Vamshi can acess the user list

➜  newhttppractice git:(main) ✗  http get "localhost:7001/api/user?username=Vamsi"

HTTP/1.1 200 OK
Content-Length: 200
Content-Type: application/json
Date: Fri, 19 May 2023 07:42:02 GMT
Server: akka-http/10.1.7

[
    {
        "id": 2,
        "name": "Anuj",
        "startTime": "2023-05-21"
    },
    {
        "id": 3,
        "name": "Prateek",
        "startTime": "2023-05-22"
    },
    {
        "id": 4,
        "name": "Maithri",
        "startTime": "2023-05-22"
    },
    {
        "id": 5,
        "name": "Vamsi",
        "startTime": "2023-05-22"
    }
]


---------> delete vamsi

➜  newhttppractice git:(main) ✗  http delete "localhost:7001/api/user?id=5"

HTTP/1.1 200 OK
Content-Length: 0
Date: Fri, 19 May 2023 07:42:26 GMT
Server: akka-http/10.1.7


------------> now he can't access


➜  newhttppractice git:(main) ✗ http get "localhost:7001/api/user?username=Vamsi"
HTTP/1.1 204 No Content
Date: Fri, 19 May 2023 07:42:36 GMT
Server: akka-http/10.1.7


 */

