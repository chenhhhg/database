docker run -d --name mysql -p 3306:3306 -v /root/mysql:/var/lib/mysql -v/root/mysql/conf.d:/etc/mysql/conf.d -e MYSQL_ROOT_PASSWORD=cgx20040211 mysql --default-authentication-plugin=mysql_native_password

/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/dss.ddl

[mysqld]
secure-file-priv= ‘’

LOAD DATA INFILE '/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/customer.tbl' INTO TABLE customer FIELDS TERMINATED BY '|';
LOAD DATA INFILE '/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/orders.tbl'   INTO TABLE ORDERS   FIELDS TERMINATED BY '|';
LOAD DATA INFILE '/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/lineitem.tbl' INTO TABLE LINEITEM FIELDS TERMINATED BY '|';
LOAD DATA INFILE '/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/nation.tbl'   INTO TABLE NATION   FIELDS TERMINATED BY '|';
LOAD DATA INFILE '/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/partsupp.tbl' INTO TABLE PARTSUPP FIELDS TERMINATED BY '|';
LOAD DATA INFILE '/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/part.tbl'     INTO TABLE PART     FIELDS TERMINATED BY '|';
LOAD DATA INFILE '/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/region.tbl'   INTO TABLE REGION   FIELDS TERMINATED BY '|';
LOAD DATA INFILE '/var/lib/mysql/tpc/TPC-H V3.0.1/dbgen/tbl/supplier.tbl' INTO TABLE SUPPLIER FIELDS TERMINATED BY '|';
