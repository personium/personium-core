select 
    cell_id,
    box_id,
    node_id,
    ent1_type,
    ent1_id,
    ent2_type,
    ent2_id,
    published,
    updated,
    id 
from `##schema##`.LINK where id in 
