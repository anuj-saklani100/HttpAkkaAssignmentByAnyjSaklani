package databases
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

import java.sql.{Connection, DriverManager, ResultSet}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._

case class User(id: Int, name: String, startTime: String)

trait superduper extends DefaultJsonProtocol {
  implicit val change = jsonFormat3(User)
}

object UserDAO extends SprayJsonSupport with superduper {

  def main(args: Array[String]): Unit = {
    val url = "jdbc:mysql://localhost:3306/mydatabase"
    val username = "root"
    val password = "Tellius@12345"

    val userDao = new UserDAO(url, username, password)

    // Use userDao to perform CRUD operations
    import akka.http.scaladsl.server.Directives._
    import spray.json._
    val requestHandler = path("api") {
      post{
       entity(as [User]){
         (a)=>{
           val res3=userDao.addUser(a).map(_ => StatusCodes.OK)
           complete(res3)
         }
       }
      }~
      patch {
        entity(as[User]) { updatedUser =>
          parameter('id.as[Int]) { userId =>
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val currentDateTimeString = currentDateTime.format(formatter)

            val us = User(userId, updatedUser.name, currentDateTimeString)
            val res1 = userDao.updateUserById(userId, us)
            complete(res1)
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
    Http().bindAndHandle(requestHandler,"localhost",9090)
  }

  class UserDAO(url: String, username: String, password: String) {
    private def getConnection(): Connection =
      DriverManager.getConnection(url, username, password)

    private def parseResultSet(resultSet: ResultSet): List[User] = {
      var users: List[User] = List.empty[User]
      while (resultSet.next()) {
        val id = resultSet.getInt("id")
        val name = resultSet.getString("name")
        val startTime = resultSet.getString("startTime")
        val user = User(id, name, startTime)
        users = user :: users
      }
      users.reverse
    }

    // Implement CRUD operations here
    def getAllUsers(): List[User] = {
      val connection = getConnection()
      val statement = connection.createStatement()
      val query = "SELECT * FROM USERS"
      val resultSet = statement.executeQuery(query)
      val users = parseResultSet(resultSet)
      resultSet.close()
      statement.close()
      connection.close()
      users
    }

    def findUserById(userId: Int): Option[User] = {
      val connection = getConnection()
      val statement = connection.createStatement()
      val query = s"SELECT * FROM USERS WHERE id = $userId"
      val resultSet = statement.executeQuery(query)
      val userOption =
        if (resultSet.next()) {
          val id = resultSet.getInt("id")
          val name = resultSet.getString("name")
          val startTime = resultSet.getString("startTime")
          Some(User(id, name, startTime))
        } else {
          None
        }
      resultSet.close()
      statement.close()
      connection.close()
      userOption
    }

    def updateUserById(userId: Int, updatedUser: User): Option[User] = {
      val connection = getConnection()
      val query = s"UPDATE USERS SET name = '${updatedUser.name}', startTime = '${updatedUser.startTime}' WHERE id = $userId"
      val statement = connection.createStatement()
      val rowsUpdated = statement.executeUpdate(query)

      println(s"Executed Query: $query")
      println(s"Rows Updated: $rowsUpdated")

      statement.close()
      connection.close()

      None
    }

    def addUser(newUser: User): Option[User] = {
      val connection = getConnection()
      val query = "INSERT INTO USERS (id, name, startTime) VALUES (?, ?, ?)"
      val preparedStatement = connection.prepareStatement(query)

      preparedStatement.setInt(1, newUser.id)
      preparedStatement.setString(2, newUser.name)
      preparedStatement.setString(3, newUser.startTime)

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
    }


  }
}
