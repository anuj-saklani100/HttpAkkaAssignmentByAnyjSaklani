package databases
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

import java.sql.{Connection, DriverManager, ResultSet}
import scala.concurrent.duration._
import scala.util.matching.Regex


case class Userpass(id: Int, name: String, password: String)
trait feature extends DefaultJsonProtocol {
  implicit val change = jsonFormat3(Userpass)
}
object passUser extends SprayJsonSupport with feature {

  def main(args: Array[String]): Unit = {
    val url = "jdbc:mysql://localhost:3306/passUser"
    val username = "root"
    val password = "Tellius@12345"

    val userDao = new passUser(url, username, password)

    // Use userDao to perform CRUD operations
    import akka.http.scaladsl.server.Directives._
    import spray.json._
    val requestHandler = path("api") {
      post{
        entity(as [Userpass]){
          (a)=>{
            val res3=userDao.addUser(a).map(_ => StatusCodes.OK)
            complete(res3)
          }
        }
      }~
        patch {
          entity(as[Userpass]) { updatedUser =>
            parameter('id.as[Int]) { userId =>


              val us = Userpass(userId, updatedUser.name, updatedUser.password)
              val res1 = userDao.updateUserById(userId, us)
              if(res1){
                complete(StatusCodes.OK)
              }else{
                complete(StatusCodes.NotAcceptable)
              }
            }
          }
        }~
        parameter('id.as[Int]){
          (a)=>{
            val res=userDao.findUserById(a)
            complete(res)
          }
        }~
        get {
          val allUsers = userDao.getAllUsers()
          complete(allUsers)
        }


    }
    implicit val system=ActorSystem("tests")
    implicit val materializer=ActorMaterializer()
    import system.dispatcher
    Http().bindAndHandle(requestHandler,"localhost",6060)
  }

  class passUser(url: String, username: String, password: String) {
    private def getConnection(): Connection =
      DriverManager.getConnection(url, username, password)

    private def parseResultSet(resultSet: ResultSet): List[ Userpass] = {
      var users: List[ Userpass] = List.empty[ Userpass]
      while (resultSet.next()) {
        val id = resultSet.getInt("id")
        val name = resultSet.getString("name")
        val password = resultSet.getString("password")
        val user =  Userpass(id, name, password)
        users = user :: users
      }
      users.reverse
    }

    // -------------------------   password validation mechanism --------------------

       private def validatePass(users:Userpass):Boolean={
         val minLength = 8
         val hasDigitRegex: Regex = ".*\\d.*".r
         val hasCharRegex: Regex = ".*[a-zA-Z].*".r
         val hasSpecialCharRegex: Regex = ".*[^a-zA-Z0-9].*".r

         val passLength=users.password.length
         val pass=users.password
        // if password satisfy all the 4 conditions
         (passLength>=minLength)& (hasDigitRegex.findFirstMatchIn(pass).isDefined)&&(hasCharRegex.findFirstMatchIn(pass).isDefined)&(hasSpecialCharRegex.findFirstMatchIn(pass).isDefined)
       }





    //-----------------------------------------------------------------------------------



    // Implement CRUD operations here
    def getAllUsers(): List[ Userpass] = {
      val connection = getConnection()
      val statement = connection.createStatement()
      val query = "SELECT * FROM Member"
      val resultSet = statement.executeQuery(query)
      val users = parseResultSet(resultSet)
      resultSet.close()
      statement.close()
      connection.close()
      users
    }

    def findUserById(userId: Int): Option[ Userpass] = {
      val connection = getConnection()
      val statement = connection.createStatement()
      val query = s"SELECT * FROM Member WHERE id = $userId"
      val resultSet = statement.executeQuery(query)
      val userOption =
        if (resultSet.next()) {
          val id = resultSet.getInt("id")
          val name = resultSet.getString("name")
          val password = resultSet.getString("password")
          Some( Userpass(id, name, password))
        } else {
          None
        }
      resultSet.close()
      statement.close()
      connection.close()
      userOption
    }

    def updateUserById(userId: Int, updatedUser: Userpass): Boolean = {
      val connection = getConnection()
      val query = s"UPDATE Member SET name = '${updatedUser.name}', password = '${updatedUser.password}' WHERE id = $userId"
      val statement = connection.createStatement()
      val rowsUpdated = statement.executeUpdate(query)
      // checking validation

      if(validatePass(updatedUser)){
        println("Password validated and update is successfully done!")
        println(s"Executed Query: $query")
        println(s"Rows Updated: $rowsUpdated")

        statement.close()
        connection.close()

        true
      }else{
        false
      }

    }

    def addUser(newUser:  Userpass): Option[ Userpass] = {
      val connection = getConnection()
      val query = "INSERT INTO Member (id, name, password) VALUES (?, ?, ?)"
      val preparedStatement = connection.prepareStatement(query)
      if(validatePass(newUser))
     {
       preparedStatement.setInt(1, newUser.id)
       preparedStatement.setString(2, newUser.name)
       preparedStatement.setString(3, newUser.password)

       val rowsInserted = preparedStatement.executeUpdate()

       if (rowsInserted > 0) {
         preparedStatement.close()
         connection.close()
         Some(newUser)
       } else {
         preparedStatement.close()
         connection.close()
         None
       }
     }else{
        None
      }
    }


  }
}



/*
------------------------------------------------- Output ------------------------------------------------
newhttppractice git:(main) ✗ http get localhost:6060/api
HTTP/1.1 200 OK
Content-Length: 2
Content-Type: application/json
Date: Fri, 19 May 2023 10:34:08 GMT
Server: akka-http/10.1.7

[]


➜  newhttppractice git:(main) ✗ http post "localhost:6060/api" <src/main/json/adder.json
HTTP/1.1 200 OK
Content-Length: 2
Content-Type: text/plain; charset=UTF-8
Date: Fri, 19 May 2023 10:35:12 GMT
Server: akka-http/10.1.7

OK


➜  newhttppractice git:(main) ✗  http get localhost:6060/api

HTTP/1.1 200 OK
Content-Length: 55
Content-Type: application/json
Date: Fri, 19 May 2023 10:35:26 GMT
Server: akka-http/10.1.7

[
  {
      "id": 4,
      "name": "Prateek",
      "password": "Prateek@Goel12"
  }
]


➜  newhttppractice git:(main) ✗  http post "localhost:6060/api" <src/main/json/updt.json

HTTP/1.1 200 OK
Content-Length: 0
Date: Fri, 19 May 2023 10:36:14 GMT
Server: akka-http/10.1.7



➜  newhttppractice git:(main) ✗ http get localhost:6060/api

HTTP/1.1 200 OK
Content-Length: 55
Content-Type: application/json
Date: Fri, 19 May 2023 10:36:28 GMT
Server: akka-http/10.1.7

[
  {
      "id": 4,
      "name": "Prateek",
      "password": "Prateek@Goel12"
  }
]


➜  newhttppractice git:(main) ✗ http post "localhost:6060/api" <src/main/json/newuser.json

HTTP/1.1 200 OK
Content-Length: 0
Date: Fri, 19 May 2023 10:37:16 GMT
Server: akka-http/10.1.7



➜  newhttppractice git:(main) ✗  http get localhost:6060/api

HTTP/1.1 200 OK
Content-Length: 55
Content-Type: application/json
Date: Fri, 19 May 2023 10:37:25 GMT
Server: akka-http/10.1.7

[
  {
      "id": 4,
      "name": "Prateek",
      "password": "Prateek@Goel12"
  }
]


➜  newhttppractice git:(main) ✗  http post "localhost:6060/api" <src/main/json/second.json

HTTP/1.1 200 OK
Content-Length: 2
Content-Type: text/plain; charset=UTF-8
Date: Fri, 19 May 2023 10:37:47 GMT
Server: akka-http/10.1.7

OK


➜  newhttppractice git:(main) ✗  http get localhost:6060/api

HTTP/1.1 200 OK
Content-Length: 115
Content-Type: application/json
Date: Fri, 19 May 2023 10:37:57 GMT
Server: akka-http/10.1.7

[
  {
      "id": 4,
      "name": "Prateek",
      "password": "Prateek@Goel12"
  },
  {
      "id": 2,
      "name": "Shahrukh Khan",
      "password": "anuj@saklani12"
  }
]


➜  newhttppractice git:(main) ✗ http patch "localhost:6060/api?id=4" <src/main/json/doingUpdate.json
HTTP/1.1 406 Not Acceptable
Content-Length: 128
Content-Type: text/plain; charset=UTF-8
Date: Fri, 19 May 2023 10:41:06 GMT
Server: akka-http/10.1.7

The requested resource is only capable of generating content not acceptable according to the Accept headers sent in the request.


➜  newhttppractice git:(main) ✗ http patch "localhost:6060/api?id=4" <src/main/json/acceptableUpdate.json

HTTP/1.1 200 OK
Content-Length: 2
Content-Type: text/plain; charset=UTF-8
Date: Fri, 19 May 2023 10:42:20 GMT
Server: akka-http/10.1.7

OK


➜  newhttppractice git:(main) ✗ http get localhost:6060/api

HTTP/1.1 200 OK
Content-Length: 123
Content-Type: application/json
Date: Fri, 19 May 2023 10:42:32 GMT
Server: akka-http/10.1.7

[
  {
      "id": 4,
      "name": "Anuj Saklani",
      "password": "Master@Tellius123"
  },
  {
      "id": 2,
      "name": "Shahrukh Khan",
      "password": "anuj@saklani12"
  }
]


➜  newhttppractice git:(main) ✗


 */
