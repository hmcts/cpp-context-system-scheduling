# Instructions for creating the database

1. Install Postgres 9.4 or later
2. Create a database systemschedulingviewstore with following commands:

		1.Open command prompt

		2.cd D:\software\PostgreSQL\9.4\bin

		3.Connect to the database: 
		a.psql -U postgres postgres

		Note:  The above command will prompt for a password, enter the password you used to setup the database. 

		4.Create user and database by entering the following queries:

		a.CREATE USER systemscheduling WITH PASSWORD 'systemscheduling';

		b.CREATE DATABASE systemschedulingviewstore OWNER systemscheduling;


		5.Quit the postgres database by entering:
		a. /q or CTRL+C

		6.Login to systemschedulingviewstore database:
		a.psql -U systemschedulingviewstore systemschedulingviewstore
		Note: The above command will prompt for a password, enter the password "password". 

		7.Create a new schema in system.schedulingcppdb database by entering the following query:

		a.CREATE SCHEMA system.schedulingcppschema;

		8.Provide relevant access to user/schema/database by entering the following commands:

		a.ALTER DATABASE systemschedulingviewstore SET search_path TO system.schedulingcppschema;
		b.ALTER user system.schedulingcpp SET search_path TO system.schedulingcppschema;
		c.GRANT ALL ON SCHEMA system.schedulingcppschema TO system.schedulingcpp ;

		9.
		Quit the systemschedulingcppdb database
		 /q or CTRL+C
 
5. Run with the following command:
   mvn -Dliquibase.url=jdbc:postgresql://localhost:5432/systemschedulingviewstore -Dliquibase.username=systemscheduling -Dliquibase.password=systemscheduling -Dliquibase.logLevel=info resources:resources liquibase:update
   
   OR go to target directory and run:
   java -jar systemscheduling-liquibase-1.0.0-SNAPSHOT.jar --url=jdbc:postgresql://localhost:5432/systemschedulingviewstore --username=system.systemscheduling --password=systemscheduling --logLevel=info --defaultSchemaName=system.schedulingcppschema  update

