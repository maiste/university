/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package poca

import scala.concurrent.Future
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import com.github.t3hnar.bcrypt._
import scala.util.Success
import scala.util.Failure

case class User(
    userId: String,
    username: String,
    password: String,
    email: String,
    isSeller: Boolean = false
)

final case class UserAlreadyExistsException(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)
final case class InconsistentStateException(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)
final case class PasswordException(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)
final case class MailException(
    private val message: String = "",
    private val cause: Throwable = None.orNull
) extends Exception(message, cause)

class Users {
  class UsersTable(tag: Tag)
      extends Table[(String, String, String, String, Boolean)](tag, "users") {
    def userId   = column[String]("userId", O.PrimaryKey)
    def username = column[String]("username")
    def password = column[String]("password")
    def email    = column[String]("email")
    def isSeller = column[Boolean]("is_seller")
    def *        = (userId, username, password, email, isSeller)
  }

  implicit val executionContext =
    scala.concurrent.ExecutionContext.Implicits.global
  val db    = MyDatabase.db
  val users = TableQuery[UsersTable]

  private def checkEmail(email: String): Boolean = {
    val regex = """.+\@.+\..+""".r
    regex.matches(email)
  }

  def createUser(
      username: String,
      unencrypted_password: String,
      email: String,
      isSeller: Boolean = false
  ): Future[Unit] = {
    val existingUsersFuture = getUserByUsername(username)

    existingUsersFuture.flatMap(existingUsers => {
      if (existingUsers.isEmpty) {
        val userId = UUID.randomUUID.toString()
        val password = unencrypted_password.bcryptSafeBounded match {
          case Success(value) => value
          case Failure(exception) =>
            throw new PasswordException("Password encryption failed")
        }

        val emailCheck = checkEmail(email) match {
          case true  => email
          case false => throw new MailException("Mail not well formed")
        }

        val newUser = User(
          userId = userId,
          username = username,
          password = password,
          email = email,
          isSeller = isSeller
        )
        val newUserAsTuple: (String, String, String, String, Boolean) =
          User.unapply(newUser).get

        val dbio: DBIO[Int]           = users += newUserAsTuple
        var resultFuture: Future[Int] = db.run(dbio)

        // We do not care about the Int value
        resultFuture.map(_ => ())
      } else {
        throw new UserAlreadyExistsException(
          s"A user with username '$username' already exists."
        )
      }
    })
  }

  def updatePassword(username: String, password: String): Future[Unit] = {
    val user = getUserByUsername(username)
    val newPassword = password.bcryptSafeBounded match {
      case Success(value) => value
      case Failure(exception) =>
        throw new PasswordException("Password encryption failed")
    }

    user.flatMap {
      case Some(u) => {
        val newUser = User(
          userId = u.userId,
          username = u.username,
          password = newPassword,
          email = u.email,
          isSeller = u.isSeller
        )
        val newUserAsTuple: (String, String, String, String, Boolean) =
          User.unapply(newUser).get
        val query: DBIO[Int] = users insertOrUpdate newUserAsTuple

        var resultFuture: Future[Int] = db.run(query)
        resultFuture.map(_ => ())
      }
      case _ => throw new PasswordException("User not found")
    }

  }

  def updateMail(username: String, email: String): Future[Unit] = {
    val user = getUserByUsername(username)
    val emailCheck = checkEmail(email) match {
      case true  => email
      case false => throw new MailException("Error in mail format")
    }

    user.flatMap {
      case Some(u) => {
        val newUser = User(
          userId = u.userId,
          username = u.username,
          password = u.password,
          email = emailCheck,
          isSeller = u.isSeller
        )
        val newUserAsTuple: (String, String, String, String, Boolean) =
          User.unapply(newUser).get
        val query: DBIO[Int]          = users insertOrUpdate newUserAsTuple
        var resultFuture: Future[Int] = db.run(query)
        resultFuture.map(_ => ())
      }
      case _ => throw new MailException("User not found")
    }

  }

  def checkUserPassword(user: User, unencrypted_password: String): Boolean = {
    unencrypted_password.isBcryptedSafeBounded(user.password) match {
      case Success(true) => true
      case _             => false
    }
  }

  def getUserByUsername(username: String): Future[Option[User]] = {
    val query = users.filter(_.username === username)

    val userListFuture = db.run(query.result)

    userListFuture.map(
      (userList: Seq[(String, String, String, String, Boolean)]) => {
        userList.length match {
          case 0 => None
          case 1 => Some(User tupled userList.head)
          case _ =>
            throw new InconsistentStateException(
              s"Username $username is linked to several users in database!"
            )
        }
      }
    )
  }

  def getUserById(id: String): Future[Option[User]] = {
    val query = users.filter(_.userId === id)

    val userListFuture = db.run(query.result)

    userListFuture.map(
      (userList: Seq[(String, String, String, String, Boolean)]) => {
        userList.length match {
          case 0 => None
          case 1 => Some(User tupled userList.head)
          case _ =>
            throw new InconsistentStateException(
              s"UserId $id is linked to several users in database!"
            )
        }
      }
    )
  }

  def deleteUserById(id: String) = {
    val query = users.filter(_.userId === id).delete
    db.run(query)
  }

  def getAllUsers(): Future[Seq[User]] = {
    val userListFuture = db.run(users.result)

    userListFuture.map(
      (userList: Seq[(String, String, String, String, Boolean)]) => {
        userList.map(User tupled _)
      }
    )
  }
}
