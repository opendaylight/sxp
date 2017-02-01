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
   elseif attr_type_value == 0x0c or attr_type_value == 0x0e then -- (add | delete) IPv6 prefix
      local mask = attr_value_raw(0, 1)
      tree:add(mask, "IPv6 mask : " .. mask:uint())
      local ip_address = attr_value_raw(1, 16)
      tree:add(mask, "IPv6 address : " .. tostring(ip_address:ipv6()))
   elseif  attr_type_value == 0x10 then -- peer sequence
      process_peer_sequence(attr_value_raw, tree)
   elseif  attr_type_value == 0x11 then -- sgt
      local sgt = attr_value_raw(0, 2)
      tree:add(sgt, "sgt : " .. sgt:uint())
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
