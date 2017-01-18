CREATE  TABLE IF NOT EXISTS `##schema##`.`CELL_DELETE` (
  `db_name` VARCHAR(128) NOT NULL ,
  `table_name` VARCHAR(40) NOT NULL ,
  `cell_id` VARCHAR(40) NOT NULL ,
  `create_date` DATETIME NOT NULL ,
  PRIMARY KEY (`db_name`, `table_name`, `cell_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 MAX_ROWS=4294967295
