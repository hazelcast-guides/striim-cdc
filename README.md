# Load data from Oracle CDC to Hazelcast via Striim

### Requirements

#### Striim

In this guide, I prefer to use [dockerized Striim](https://hub.docker.com/r/striim/evalversion/) that is maintained by Striim:

```
$ docker pull striim/evalversion
$ docker run -d --name striim -p 9080:9080 -e "STRIIM_ACCEPT_EULA=Y"  striim/evalversion
```

Striim dashboard will be available under `http://localhost:9080`, you can use `admin/admin` as a credentials.

#### Oracle Database

As in Strimm, I also prefer [dockerized Oracle Database](https://hub.docker.com/_/oracle-database-enterprise-edition). To pull the docker image, you need to `Proceed Checkout`. 

You can follow installation steps at official DockerHub page or below ones:

```bash
$ docker pull store/oracle/database-enterprise:12.2.0.1
$ docker run -d -p 8080:8080 -p 1521:1521 --name oracledb store/oracle/database-enterprise:12.2.0.1
# until see the "SQL> ORACLE instance started." log
$ watch docker logs oracledb

# Check DB actually works
$ docker exec -it oracledb bash -c "source /home/oracle/.bashrc; sqlplus /nolog"
```


### Configuring Oracle Database

To create `OracleReader` at Striim without any issue, you need to make some changes on your Oracle DB. You can find all details, [here](https://www.striim.com/docs/en/oracle-configuration.html). While following these steps, keep in you mind that newly created Oracle DB's version is `12c` and it is a `multi-tenant DB(CDB)`.

In addition to steps are defined by Striim official documentation, you need to create another user to create/update database tables and create `PRODUCT_INV` table with the user. We will use/populate the newly created table at next steps. By the way, this user will be used by our Spring application.

Create user:
```bash
# Enter 'Oradoc_db1' as a password
$ SQL> conn sys as sysdba;

$ SQL> alter session set container=ORCLPDB1;
$ SQL> create user striim identified by striim;
$ SQL> grant connect, resource,dba to striim container=current;
$ SQL> alter user striim default role dba;
```
Create table:
```bash
$ SQL> conn striim/striim@orclpdb1
$ SQL> create table STRIIM.PRODUCT_INV(SKU NUMBER(19) not null primary key, LAST_UPDATED TIMESTAMP(6), NAME VARCHAR2(255 char), STOCK FLOAT not null);
```

### Configuring Striim

 1) Download driver jar(`ojdbc8.jar`) from [Oracle website](https://www.oracle.com/database/technologies/jdbc-ucp-122-downloads.html). `OracleReader` needs this jar to connect DB so follow the steps below to add it into `striim` container:

    ```bash
    $ docker cp ojdbc8.jar striim:/opt/striim/lib/ojdbc8.jar
     
    $ docker exec -it striim bash
    $ chmod +x /opt/striim/lib/ojdbc8.jar
    $ chown striim:striim /opt/striim/lib/ojdbc8.jar
    ```

 2) Add Maven Oracle JDBC driver in your Maven local repository then build the project to create `pojo-0.0.1-SNAPSHOT.jar`:
    ```bash
    $ mvn install:install-file -Dfile=path/to/your/ojdbc8.jar -DgroupId=com.oracle 
        -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
    
    $ mvn clean install
    ```

 3) To use `HazelcastWriter`, you need to POJO jar and ORM file, you can find details about these files, [here](https://www.striim.com/docs/en/hazelcast-writer.html). You can copy POJO jar and ORM file to striim container via below commands:

    ```bash
    $ docker cp ./pojo/target/pojo-0.0.1-SNAPSHOT.jar striim:/opt/striim/lib/pojo-0.0.1-SNAPSHOT.jar
    $ docker cp ./config/product_inv_orm.xml striim:/opt/striim/

    $ docker exec -it striim bash
    $ chmod +x /opt/striim/lib/pojo-0.0.1-SNAPSHOT.jar
    $ chown striim:striim /opt/striim/lib/pojo-0.0.1-SNAPSHOT.jar
    $ chown striim:striim  productInv_orm.xml
    ```

 4) After all changes restart your container and proceed to the next steps:
    ```bash
    $ docker restart striim
    ```

### (Quick Setup) Use existing TQL file

If you do not want to bypass next two sections, you can import pre-prepared TQL file.

 1) Change `{ORACLE_DB_ADDRESS}` and `{HZ_IP_ADDRESS}` placeholders with real IP addresses. You can also modify these values before deploy the app.
 
 2) To create new app, select `Import Existing App` and choose tql which you modified.
 

### Configuring Oracle Database CDC connection from Striim dashboard

 1) To create new app, select `Start with Template` then `Oracle CDC to Hazelcast`:

    ![Create New App](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/create_new_app.png)
    
    Use `OracleHazelcastCDC` or another name as an `Application Name`. 
    
 2) Enter your Oracle DB data and credentials:
    ![DB Connection Creds](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/oracle_reader_1)
    ![DB Connection Control](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/oracle_reader_2)
    
 3) Select source table:
    ![Source Table](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/oracle_reader_2)
    
### Configuring Hazelcast

...


### Start Spring Boot Application to populate a database 

```bash
git clone https://github.com/hasancelik/hazelcast-striim-cdc
```