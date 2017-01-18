CREATE  TABLE IF NOT EXISTS `##schema##`.`ENTITY` (
  `id` VARCHAR(40) BINARY NOT NULL ,
  `type` VARCHAR(200) NOT NULL ,
  `cell_id` VARCHAR(40) NULL ,
  `box_id` VARCHAR(40) NULL ,
  `node_id` VARCHAR(40) NULL ,
  `entity_id` VARCHAR(40) NULL ,
  `declared_properties` LONGTEXT NULL ,
  `dynamic_properties` LONGTEXT NULL ,
  `hidden_properties` TEXT NULL ,
  `links` LONGTEXT NULL ,
  `published` BIGINT UNSIGNED NULL ,
  `updated` BIGINT UNSIGNED NULL ,
  INDEX idx_cell_id(`cell_id`),
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 MAX_ROWS=4294967295
