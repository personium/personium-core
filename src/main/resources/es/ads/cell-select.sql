select
  type,
  cell_id,
  box_id,
  node_id,
  declared_properties,
  dynamic_properties,
  hidden_properties,
  links,
  acl,
  published,
  updated,
  id
from `##schema##`.CELL limit ?, ?
