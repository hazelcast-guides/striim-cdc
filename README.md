# Load data from Oracle CDC to Hazelcast via Striim

This guide explains How to use Hazelcast Striim Writer to cache data stored in Oracle Database Enterprise Edition(12.2.0.1) .

### Prerequisites
- Docker
- DockerHub Account (required by Oracle Database Enterprise Edition)

### Installation

- Oracle Database Enterprise Edition
- Striim Evaluation Edition
- Hazelcast and Management Center


#### Striim

Striim is an end-to-end streaming platform for real-time integration and analytics of data.
This guide uses Striim evaluation version hosted at [Striim DockerHub](https://hub.docker.com/r/striim/evalversion/)

To run Striim evaluation version, run following docker commands
```
$ docker pull striim/evalversion
$ docker run -d --name striim -p 9080:9080 -e "STRIIM_ACCEPT_EULA=Y"  striim/evalversion
```

make sure that Striim started properly.
```
$ docker logs striim -f
...
started.
Please go to http://172.17.0.2:9080 or https://172.17.0.2:9081 to administer, or use console
```

That is excerpt from Striim DockerHub Page if you experience an expired license key issue.
```
Note: This docker image is an evaluation version which comes with a trial license valid for 15 days and single node. Any docker image older than 15 days would not work. We try and keep pushing our latest releases on docker hub. But in case, you don't see an image within 15 days or if you need extended license, please contact support@striim.com
```

If everything goes well then Striim dashboard will be available under [http://localhost:9080](http://localhost:9080). You can use `admin/admin` as a credentials.

#### Oracle Database Enterprise Edition

Oracle Database Docker Hub Page contains a docker Image as container option for Oracle Database. Visit [Oracle Database Docker Hub Page](https://hub.docker.com/_/oracle-database-enterprise-edition) and click on `Proceed Checkout`. This will subscribe you to Developer Tier.

Make sure you are logged in to DockerHub
```
$ docker login
Authenticating with existing credentials...
Login Succeeded

```

Run Oracle Database in a Docker Container
```
$ docker pull store/oracle/database-enterprise:12.2.0.1
$ docker run -d -p 8080:8080 -p 1521:1521 --name oracledb store/oracle/database-enterprise:12.2.0.1
```

Check logs until you see `SQL> ORACLE instance started.`

```
$ watch docker logs oracledb
```

### Hazelcast and Management Center

Start Hazelcast member and Management Center containers:

```bash
$ docker run -d --name mancenter -p 38080:8080 hazelcast/management-center:3.12.6

# extract management-center container's IP address
$ docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' mancenter

$ docker run -d --name hazelcast -p 5701:5701 -e MANCENTER_URL="http://{MAN_CENTER_CONTAINER_IP}:8080/hazelcast-mancenter"  hazelcast/hazelcast:3.12.6
```

Management Center dashboard will be avaliable under `http://localhost:38080/hazelcast-mancenter`.


Check all containers are up and ready before continue to manual:
```bash
$ docker ps
IMAGE                                       STATUS                    PORTS                                                      NAMES
hazelcast/hazelcast:3.12.6                  Up 16 minutes             0.0.0.0:5701->5701/tcp                                     hazelcast
hazelcast/management-center:3.12.6          Up 23 minutes             8081/tcp, 8443/tcp, 0.0.0.0:38080->8080/tcp                mancenter
striim/evalversion                          Up 25 minutes             1527/tcp, 0.0.0.0:9080->9080/tcp                           striim
store/oracle/database-enterprise:12.2.0.1   Up 25 minutes (healthy)   0.0.0.0:1521->1521/tcp, 0.0.0.0:8080->8080/tcp, 5500/tcp   oracledb
```

### Configuring Oracle Database

Connect to Oracle DB
```bash
$ docker exec -it oracledb bash -c "source /home/oracle/.bashrc; sqlplus /nolog"

# Enter 'Oradoc_db1' as a password
$ SQL> conn sys as sysdba;
```

Enable `ARCHIVELOG` by following this [link] (https://www.striim.com/docs/en/enabling-archivelog.html)

Enable `SUPPLEMENTAL LOG DATA` by following this [link] (https://www.striim.com/docs/en/enabling-supplemental-log-data.html)

Create an Oracle User with LogMiner Privileges 
```
create role c##striim_privs;
grant create session,execute_catalog_role,select any transaction,select any dictionary,logmining to c##striim_privs;
grant select on SYSTEM.LOGMNR_COL$ to c##striim_privs;
grant select on SYSTEM.LOGMNR_OBJ$ to c##striim_privs;
grant select on SYSTEM.LOGMNR_USER$ to c##striim_privs;
grant select on SYSTEM.LOGMNR_UID$ to c##striim_privs;
create user c##striim identified by striim container=all;
grant c##striim_privs to c##striim container=all;
alter user c##striim set container_data = (cdb$root, ORCLPDB1) container=current;
```

Create striim user to create/update database tables
```bash
# Enter 'Oradoc_db1' as a password
$ SQL> conn sys as sysdba;

$ SQL> alter session set container=ORCLPDB1;
$ SQL> create user striim identified by striim;
$ SQL> grant connect, resource,dba to striim container=current;
$ SQL> alter user striim default role dba;
```
Create create `PRODUCT_INV` table with striim user :
```bash
$ SQL> conn striim/striim@orclpdb1
$ SQL> create table STRIIM.PRODUCT_INV(SKU NUMBER(19) not null primary key, LAST_UPDATED TIMESTAMP(6), NAME VARCHAR2(255 char), STOCK FLOAT not null);
```
We will use/populate the newly created table at next steps. By the way, this user will be used by our Spring application.

### Configuring Striim

 1) Download driver jar(`ojdbc8.jar`) from [Oracle website](https://www.oracle.com/database/technologies/jdbc-ucp-122-downloads.html). `OracleReader` needs this jar to connect DB so follow the steps below to add it into `striim` container:

    ```bash
    $ docker cp path/to/your/ojdbc8.jar striim:/opt/striim/lib/ojdbc8.jar
     
    $ docker exec -it striim chown striim:striim /opt/striim/lib/ojdbc8.jar
    $ docker exec -it striim chmod +x /opt/striim/lib/ojdbc8.jar
    ```
    
    install the jar into your local maven repository
    ```bash
    $ mvn install:install-file -Dfile=path/to/your/ojdbc8.jar -DgroupId=com.oracle 
     -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
    ```

 2) Clone the project then build the project to create `pojo-0.0.1-SNAPSHOT.jar`:
    
    ```bash
     $ git clone https://github.com/hazelcast-guides/striim-hazelcast-cdc.git
     $ cd pojo
     $ mvn clean install
    ```

 3) To use `HazelcastWriter`, you need to POJO jar and ORM file, you can find details about these files, [here](https://www.striim.com/docs/en/hazelcast-writer.html). You can copy POJO jar and ORM file to striim container via below commands:

    ```bash
    $ docker cp ./pojo/target/pojo-0.0.1-SNAPSHOT.jar striim:/opt/striim/lib/pojo-0.0.1-SNAPSHOT.jar
    $ docker cp ./config/product_inv_orm.xml striim:/opt/striim/

    $ docker exec -it striim chown striim:striim /opt/striim/lib/pojo-0.0.1-SNAPSHOT.jar
    $ docker exec -it striim chown striim:striim /opt/striim/product_inv_orm.xml
    $ docker exec -it striim chmod +x /opt/striim/lib/pojo-0.0.1-SNAPSHOT.jar
    ```

 4) After all changes restart your container and proceed to the next steps:
    ```bash
    $ docker restart striim
    ```
## Install OracleHazelcastCDC App into Striim

In previous sections, we installed required software for the sample application. We now will install OracleHazelcastCDC app through Striim dashboard.
There are two ways to install OracleHazelcastCDC App. We recommend to use `Using TQL file (Quick Setup)`

- Using TQL file (Quick Setup)
- Configuring through Striim dashboard - [instructions](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/striim_dashboard.md)

###  Configuring Oracle Database CDC connection Using TQL file (Quick Setup)

 1) Change `{ORACLE_DB_ADDRESS}` and `{HZ_IP_ADDRESS}` placeholders with your HOST IP addresses at `config/OracleHazelcastCDC.tql`. You can modify these values before deploy the app as well.       
    
 2) Go to [Create App Page](http://localhost:9080/#createapp) and select `Import Existing App` and choose `.tql` which you already modified.
 
 3) Deploy and Run CDC application:
 
 ![Run CDC Application](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_run_2.png)

## Start Spring Boot Application to populate a database
   
 Run `spring-boot` application:
  
   ```bash
   $ mvn spring-boot:run
   ```

## Check up

 1) Check application loading data from OracleReader to HazelcastWriter. Verify throughput on the the screen with a similar number like `46 msg/s` on the screenshot below.
 
    ![Running Application](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_run_1.png)

 2) Check Hazelcast Map(`ProductInv`) size from Management Center, `http://localhost:38080/hazelcast-mancenter/dev/maps/ProductInv`:
 
    ![ProductInv Map](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/mancenter_map.png)
    
