CREATE  TABLE IF NOT EXISTS `##schema##`.`LINK` (
  `id` VARCHAR(81) BINARY NOT NULL ,
  `cell_id` VARCHAR(40) NULL ,
  `box_id` VARCHAR(40) NULL ,
  `node_id` VARCHAR(40) NULL ,
  `ent1_type` VARCHAR(200) NOT NULL ,
  `ent1_id` VARCHAR(40) NOT NULL ,
  `ent2_type` VARCHAR(200) NOT NULL ,
  `ent2_id` VARCHAR(40) NOT NULL ,
  `updated` BIGINT UNSIGNED NULL ,
  `published` BIGINT UNSIGNED NULL ,
  INDEX idx_cell_id(`cell_id`),
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 MAX_ROWS=4294967295
