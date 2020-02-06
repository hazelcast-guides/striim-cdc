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
    
#### Configuring Hazelcast Writer on Striim dashboard

 1) Put ORM file location(`/opt/striim/product_inv_orm.xml`) and Hazelcast cluster infos:
 
    ![Hazelcast Connection](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/hazelcast_writer_1.png)

 2) Check ORM mapping details:
 
    ![ORM Mapping](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/hazelcast_writer_2.png)
    
 3) Choose related `DataStream` from `Input From` dropdown and save `Target`:
 
    ![Hazelcast Target](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/hazelcast_writer_3.png)
    

#### Apply OracleReader changes and Deploy&Run the CDC application 

- (Only for Template Users) After all configuration steps finally your CDC applications is created. Before deploy and create application, as mentioned at `Configuring Oracle Database CDC connection on Striim dashboard` section, you need to update `Connection URL` and `Tables` section like this to run CDC application without any issue:

  ![Update Reader](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_change_1.png)
  
- (Only for Template Users) As a final step, go to enable OracleReader's `Support PDB and CDB` option:

  ![Enable CDB Support](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_change_2.png)

- (For both Template and Quick Setup Users) Deploy and Run CDC application:

  ![Run CDC Application](https://github.com/hazelcast-guides/striim-hazelcast-cdc/blob/master/images/application_run_2.png)
