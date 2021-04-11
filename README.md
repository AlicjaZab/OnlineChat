**Online Chat**

Simple online chat project, made under the supervision of JetBrains Academy (https://hyperskill.org/projects/91?track=1).

Key Words : Sockets, multithreading, database (SQLite), Gradle

Input/output managed through console. To start application first start the server, then start as many clients as you want and communicate thorught them. Note, that you should change path to database in Server.java file (String url variable).
Project supports authorization, chatting between users, and some more small operations.  There is permanently added an admin user, who can add and remove moderator privileges and ban users (login: admin, password:12345678). Moderators beyond the privileges of a normal user, can only ban users.


In the application commands for sever start with a "/". If you type an incorrect/inaccessible command, you will always get an appropriate message. 

Firstly, to enter the chat you have to register or login:
* `/register [login] [password]` - where login must be unique and password should have a least 8 characters;
* `/auth [login] [password]` - where there should be already an user with given login and password in the database;
* All other commands are unavailable until you log in.

When you are already authoized, you have acces to other commands:
* `/list` - displays logins of users online;
* `/chat [login]` - where login must be an online user; enters a chat with a specified user. When you do that, you can freely write messages without any command. Moreover there will be displayed some recent messages with that person, and the new ones will be marked with `(new)` tag.
* `/unread` - displays logins of authors of your unread messages;
* `/exit` - exit the chat application.

Commands available when you have chosen the user to chat with:
* `/history [number]` - displays messages from the past, from the given number and the next 25 messages (or less if conversation doesn't have enough);
* `/stats` - displays statistics for current conversation: number of all messages, number of sent and received messages.

Commands available for moderators and admin:
* `/kick [login]` - where login must represent an online user, you cannot kick out yourself, other moderator nor an admin. When you ban an user he is being logged out and cannot log in for the next 5 minutes.

Commands available only for admin:
* `/grant [login]` - where login must represent a normal user (not a modeartor nor an admin); assigns moderator privileges to a user;
* `/revoke [login]` - where login must represent a moderator; removes moderator privileges from a user.


**Example of application input and output for an admin (from JetBrains Academy)**
 (note: lines starting with `>` symbol represent user input)
```
Client started!
Server: authorize or register
> /register admin 12341234
Server: The user admin already exist!
> /auth admin 12345678
Server: you are authorized successfully!
> /unread
Server: unread from: Cat Killer
> /list
Server: online: Cat Dog Killer
> /kick Killer
Server: Killer was kicked!
> /list
Server: online: Cat Dog
> /kick admin
Server: You can't kick yourself!
> /chat Cat
Cat: Meow!!
admin: Hi cat! How're you doing?
Cat: Meow... Meow!
admin: Ok, I need to go. Bye.
(new) Cat: Meow! Meow! Meow!
> Oh, I see what I missed. Want to be a moderator?
admin: Oh, I see what I missed. Want to be a moderator?
Cat: MEOW!!!
> OK, one second
admin: OK, one second
> /grant Cat
Server: Cat is the new moderator!
Cat: Meow!!!
> /unread
Server: unread from: Dog Killer
> /chat Dog
admin: Hi Dog!
Dog: Woof! Woof!
admin: Ok, I need to go. Bye.
Dog: Woof!
(new) Dog: WOOF! WOOOF! WOOF!
(new) Dog: WOOF!
> Cat wants to kick you?
admin: Cat wants to kick you?
Dog: WOOF!
> Ok, it was a mistake...
admin: Ok, it was a mistake...
> /revoke Cat
Server: Cat is no longer a moderator!
> /stats
Server:
Statistics with Dog:
Total messages: 9
Messages from admin: 4
Messages from Dog: 5
> /history 3
Server:
admin: Cat wants to kick you?
Dog: WOOF!
admin: Ok, it was a mistake...
> /history qwe
Server: qwe is not a number!
> /unread
Server: unread from: Cat Killer
Dog: Woof, woof, woof!
> Bye!
admin: Bye!
> /exit
```
