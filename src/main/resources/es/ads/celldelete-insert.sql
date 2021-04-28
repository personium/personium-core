insert into `##schema##`.CELL_DELETE(
  db_name,
  table_name,
  cell_id,
  create_date
) values (?,?,?,SYSDATE())
