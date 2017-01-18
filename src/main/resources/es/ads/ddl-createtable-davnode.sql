CREATE  TABLE IF NOT EXISTS `##schema##`.`DAV_NODE` (
  `id` VARCHAR(40) BINARY NOT NULL ,
  `cell_id` VARCHAR(40) NULL ,
  `box_id` VARCHAR(40) NULL ,
  `node_type` VARCHAR(40) NULL ,
  `parent_id` VARCHAR(40) NULL ,
  `children` LONGTEXT NULL ,
  `acl` LONGTEXT NULL ,
  `properties` LONGTEXT NULL ,
  `file` TEXT NULL ,
  `published` BIGINT UNSIGNED NULL ,
  `updated` BIGINT UNSIGNED NULL ,
  INDEX idx_cell_id(`cell_id`),
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 MAX_ROWS=4294967295
