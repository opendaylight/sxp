-- sxp4: OPEN + OPEN_RESP messages handling

require "sxp4-common"

function handle_open_msg(payloadBuffer, subtree)
   local offset = 0
   local version = payloadBuffer(offset, 4)
   subtree:add(version, "version: " .. version:uint())
   offset = offset + 4
   local sxp_mode = payloadBuffer(offset, 4)
   subtree:add(sxp_mode, "mode: " .. sxp_mode:uint())
   offset = offset + 4

   -- handle attributes
   while offset < payloadBuffer:len() do
      local attr_tree, attr_type_value, attr_value_raw, header_offset = process_attribute_header(payloadBuffer, subtree, offset)
      offset = header_offset
      process_open_attribute_value(attr_value_raw, attr_type_value, attr_tree)
   end       
end


function process_open_attribute_value(attr_value_raw, attr_type_value, tree)
   if attr_type_value == 0x05 then -- node-id
      tree:add(attr_value_raw, SXP_ATTRIBUTE_TYPES[attr_type_value] .. ": 0x" .. tostring(attr_value_raw))
   elseif attr_type_value == 0x06 then -- capabilities
      process_capabilities(attr_value_raw, tree)
   elseif attr_type_value == 0x07 then -- hold-timer
      local value_size = attr_value_raw:len()
      if value_size == 2 then -- short (min)
         tree:add(attr_value_raw, SXP_ATTRIBUTE_TYPES[attr_type_value] .. ".min: " .. attr_value_raw:uint())
      elseif value_size == 4 then -- long (min, max)
         local timer_min = attr_value_raw(0,2)
         local timer_max = attr_value_raw(2,2)
         tree:add(timer_min, SXP_ATTRIBUTE_TYPES[attr_type_value] .. ".min: " .. timer_min:uint())
         tree:add(timer_max, SXP_ATTRIBUTE_TYPES[attr_type_value] .. ".max: " .. timer_max:uint())
      else
         tree:add(attr_value_raw, SXP_ATTRIBUTE_TYPES[attr_type_value] .. ": VIOLATED FIELD LENGTH (!=4)")
      end
   end
end


function process_capabilities(capBuffer, tree)
   local offset = 0
   while offset < capBuffer:len() do
      local cap_start = offset
      local cap_value = "n/a"
      local cap_code = capBuffer(offset, 1)
      offset = offset + 1
      local cap_length = capBuffer(offset, 1)
      offset = offset + 1
      local cap_length_value = cap_length:uint()
      if (cap_length_value > 0) then
         local cap_value_raw = capBuffer(offset, 1)
         offset = offset + 1
         cap_value = cap_value_raw:uint()
      end
      local cap_code_value = cap_code:uint()
      local subTree = tree:add(capBuffer(cap_start, 1), "cap.code: " .. SXP_CAPABILITY_CODES[cap_code_value] .. " [" .. cap_code_value .. "]")
      subTree:add(cap_code, "code: " .. cap_code_value)
      subTree:add(cap_length, "length: " .. cap_length_value)
      if cap_length_value > 0 then
         subTree:add(cap_value_raw, "value: " .. cap_value)
      end
      subTree:set_len(2 + cap_length_value)
   end
end
