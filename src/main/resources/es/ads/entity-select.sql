select 
    type,
    cell_id,
    box_id,
    node_id,
    entity_id,
    declared_properties,
    dynamic_properties,
    hidden_properties,
    links,
    published,
    updated,
    id 
from `##schema##`.ENTITY limit ?, ?
