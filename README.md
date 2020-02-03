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
    $ docker cp path/to/your/ojdbc8.jar striim:/opt/striim/lib/ojdbc8.jar
     
    $ docker exec -it striim chown striim:striim /opt/striim/lib/ojdbc8.jar
    $ docker exec -it striim chmod +x /opt/striim/lib/ojdbc8.jar
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

### (Quick Setup) Use existing TQL file

If you do not want to skip next two sections, you can import pre-prepared TQL file.

 1) Change `{ORACLE_DB_ADDRESS}` and `{HZ_IP_ADDRESS}` placeholders with your HOST IP addresses at `config/OracleHazelcastCDC.tql`. You can modify these values before deploy the app as well.       
    
 2) To create new app, select `Import Existing App` and choose `.tql` which you already modified.
 
 3) Proceed to `Apply OracleReader changes and Deploy&Run the CDC application` section.
 

### Configuring Oracle Database CDC connection on Striim dashboard

 1) To create new app, select `Start with Template` then `Oracle CDC to Hazelcast`:

    ![Create New App](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/create_new_app.png)
    
    Use `OracleHazelcastCDC` or another name as an `Application Name`. 
    
 2) Enter your Oracle DB data and credentials:
    ![DB Connection Creds](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/oracle_reader_1.png)
    ![DB Connection Control](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/oracle_reader_2.png)
    
    - `localhost` or `IP address of oracledb container` does not work for `Connection URL` so you need to use your HOST IP address.
    - As you can see above, service section of `Connection URL` is configured as a `/orclpdb1.localdomain`, not as `:ORCLCDB`. If you configure service as a `:ORCLCDB`, `STRIIM` application or `C##STRIIM` common user can not reach/list `PRODUCT_INV` table which is under `STRIIM` local user because of `CDB specific` bug at Striim template itself. We will update these infos with the correct ones before deploy the application. By the way, we have already contacted with them and reported this issue. They will provide to fix at future releases. If you use Oracle DB **without CDB**, you are not affect bt this issue.  
    
 3) Select source table:
    ![Source Table](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/oracle_reader_2.png)
    
### Configuring Hazelcast Writer on Striim dashboard

 1) Put ORM file location(`/opt/striim/product_inv_orm.xml`) and Hazelcast cluster infos:
 
    ![Hazelcast Connection](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/hazelcast_writer_1.png)

 2) Check ORM mapping details:
 
    ![ORM Mapping](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/hazelcast_writer_2.png)
    
 3) Choose related `DataStream` from `Input From` dropdown and save `Target`:
 
    ![Hazelcast Target](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/hazelcast_writer_3.png)
    

### Apply OracleReader changes and Deploy&Run the CDC application 

- (Only for Template Users) After all configuration steps finally your CDC applications is created. Before deploy and create application, as mentioned at `Configuring Oracle Database CDC connection on Striim dashboard` section, you need to update `Connection URL` and `Tables` section like this to run CDC application without any issue:

  ![Update Reader](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_change_1.png)
  
- (Only for Template Users) As a final step, go to enable OracleReader's `Support PDB and CDB` option:

  ![Enable CDB Support](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_change_2.png)

- (For both Template and Quick Setup Users) Deploy and Run CDC application:

  ![Run CDC Application](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_run_2.png)

### Start Spring Boot Application to populate a database

 1) Add Oracle JDBC driver in your Maven local repository:
   
   ```bash
   $ mvn install:install-file -Dfile=path/to/your/ojdbc8.jar -DgroupId=com.oracle 
    -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
   ```
 2) Run `spring-boot` application:
  
   ```bash
   $ mvn spring-boot:run
   ```

### Check up

 1) Check application loading data from OracleReader to HazelcastWriter:
 
    ![Running Application](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_run_1.png)

 2) Check Hazelcast Map(`ProductInv`) size from Management Center, `http://localhost:38080/hazelcast-mancenter/dev/maps/ProductInv`:
 
    ![ProductInv Map](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/mancenter_map.png)
    
