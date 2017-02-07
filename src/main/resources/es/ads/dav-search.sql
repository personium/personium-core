select
    cell_id,
    box_id,
    parent_id,
    children,
    node_type,
    acl,
    properties,
    file,
    published,
    updated,
    id
from `##schema##`.DAV_NODE where id in
