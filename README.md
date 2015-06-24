# Frisbi: api #

### Enviroment

* [MongoDB](https://www.mongodb.org/)
* [Neo4j](http://neo4j.com/)
* [Scala 2.11 / sbt 0.13](http://www.scala-sbt.org/) 



### Getting up and running ###
#### mongodb
start mongodb `mongodb/bin/mongod`
#### neo4j
start neo4j `neoj4/bin/neo4j start`. 

Go to `http://localhost:7474/browser/`  and run 

  ``` CREATE CONSTRAINT ON (u:User) ASSERT u.username IS UNIQUE; ```  
  
  then

  ``` CREATE CONSTRAINT ON (u:User) ASSERT u.email IS UNIQUE; ```
  
  then
  
```
  CREATE(users:Group{name:"users"})
  CREATE(admins:Group{name:"admins"})
  CREATE(deleted_users:Group{name:"deleted_users"})
  CREATE(deleted_topics:Group{name:"deleted_topics"})
```

Helpful queries 
```
MATCH ()-[r:IS_PARTICIPANT]->() set r.active=false RETURN r;
```

build and start `yodals-api`. In `yodals-api` run `sbt` and then `rf1`  



### tips 
#### Delete user from neo4j

http://localhost:7474/browser/
```
  MATCH (n:User {username:'kaspar'})
          OPTIONAL MATCH (n)-[r]-()
          DELETE n,r

```