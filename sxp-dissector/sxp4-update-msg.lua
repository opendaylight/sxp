-- sxp4: UPDATE message handling

require "sxp4-common"

function handle_update_msg(payloadBuffer, subtree)
    local offset = 0
    -- handle attributes
    while offset < payloadBuffer:len() do
        local attr_tree, attr_type_value, attr_value_raw, header_offset = process_attribute_header(payloadBuffer, subtree, offset)
        offset = header_offset
        process_update_attribute_value(attr_value_raw, attr_type_value, attr_tree)
    end
end


function process_update_attribute_value(attr_value_raw, attr_type_value, tree)
    if attr_type_value == 0x0b or attr_type_value == 0x0d then -- (add | delete) IPv4 prefix
        local mask = attr_value_raw(0, 1)
        tree:add(mask, "IPv4 mask : " .. mask:uint())
        local ip_address = attr_value_raw(1, 4)
        tree:add(mask, "IPv4 address : " .. tostring(ip_address:ipv4()))
    elseif attr_type_value == 0x01 or attr_type_value == 0x02 then -- (add | delete) IPv4
        local ip_address = attr_value_raw(0, 4)
        process_tlv(attr_value_raw, tree, 4)
        tree:add(ip_address, "IPv4 address : " .. tostring(ip_address:ipv4()))
    elseif attr_type_value == 0x0c or attr_type_value == 0x0e then -- (add | delete) IPv6 prefix
        local mask = attr_value_raw(0, 1)
        tree:add(mask, "IPv6 mask : " .. mask:uint())
        local ip_address = attr_value_raw(1, 16)
        tree:add(mask, "IPv6 address : " .. tostring(ip_address:ipv6()))
    elseif attr_type_value == 0x03 or attr_type_value == 0x04 then -- (add | delete) IPv6
        local ip_address = attr_value_raw(0, 16)
        process_tlv(attr_value_raw, tree, 16)
        tree:add(ip_address, "IPv6 address : " .. tostring(ip_address:ipv6()))
    elseif attr_type_value == 0x10 then -- peer sequence
        process_peer_sequence(attr_value_raw, tree)
    elseif attr_type_value == 0x11 then -- sgt
        local sgt = attr_value_raw(0, 2)
        tree:add(sgt, "sgt : " .. sgt:uint())
    else
        tree:add(attr_type_value, "unknown: " .. attr_value_raw)
    end
end


function process_peer_sequence(peerBuffer, tree)
    local offset = 0
    while offset < peerBuffer:len() do
        local peer = peerBuffer(offset, 4)
        tree:add(peer, "peer: 0x" .. tostring(peer))
        offset = offset + 4
    end
end


function process_tlv(data, tree, address_offset)
    local offset = address_offset
    local implicit_tlv_prefix_length = true;
    while offset < data:len() do
        local type = data(offset, 4):uint()
        offset = offset + 4
        local len = data(offset, 4):uint()
        offset = offset + 4
        local value = data(offset, len)
        if type == 1 then -- sgt
            tree:add(value, "sgt : " .. value:uint())
        elseif type == 2 then -- prefix length
            tree:add(value, "IPv" .. (address_offset == 4 and "4" or "6") .. " mask : " .. value:uint())
            implicit_tlv_prefix_length = false
        end
        offset = offset + len
    end
    if implicit_tlv_prefix_length then -- prefix length
        tree:add("IPv" .. (address_offset == 4 and "4" or "6") .. " mask : " .. (address_offset == 4 and "32" or "128"))
    end
end
