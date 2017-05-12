-- sxp4: common functions and constants

SXP_HEADER_LENGTH = 8
SXP_MSG_TYPE = {
    [1] = "OPEN",
    [2] = "OPEN_RESP",
    [3] = "UPDATE",
    [4] = "ERROR",
    [5] = "PURGE_ALL",
    [6] = "KEEP_ALIVE"
}
SXP_ATTRIBUTE_FLAGS = { "O", "N", "P", "C", "E" }
SXP_ATTRIBUTE_FLAG_NAMES = { ["O"] = 1, ["N"] = 2, ["P"] = 3, ["C"] = 4, ["E"] = 5 }
SXP_ATTRIBUTE_FLAGS_DESC = {
    ["O"] = "O - optional",
    ["N"] = "N - non-transitive",
    ["P"] = "P - partial",
    ["C"] = "C - compact",
    ["E"] = "E - extended"
}
SXP_ATTRIBUTE_TYPES = {
    [0x05] = "node-id",
    [0x06] = "capabilities",
    [0x07] = "hold-timer",
    [0x0b] = "add-IPv4-prefix",
    [0x01] = "add-IPv4",
    [0x0c] = "add-IPv6-prefix",
    [0x02] = "add-IPv6",
    [0x0d] = "delete-IPv4-prefix",
    [0x03] = "delete-IPv4",
    [0x0e] = "delete-IPv6-prefix",
    [0x04] = "delete-IPv6",
    [0x10] = "peer-sequence",
    [0x11] = "sgt"
}
SXP_CAPABILITY_CODES = {
    "IPv4 unicast",
    "IPv6 unicast",
    "subnet bindings"
}
SXP_ERROR_CODES = {
    "Message Header Error",
    "OPEN Message Error",
    "UPDATE Message Error"
}
SXP_ERROR_SUB_CODES = {
    "Malformed Attribute List",
    "Unexpected Attribute",
    "Missing Well-known Attribute",
    "Attribute Flags Error",
    "Attribute Length Error",
    "Malformed Attribute",
    "Optional Attribute Error",
    "Unsupported Version Number",
    "Unsupported Optional Attribute",
    "Unacceptable Hold Time"
}


function process_flags(payloadBuffer, tree, offset)
    local attr_flags_raw = payloadBuffer(offset, 1)
    local attr_flags_value = attr_flags_raw:uint()
    local attr_flags = read_attr_flags(bit.tobit(attr_flags_value))
    local flagTree = tree:add(attr_flags_raw, "flags: " .. attr_flags_tostring(attr_flags))
    for idx, isFlagSet in pairs(attr_flags) do
        if isFlagSet then
            flagTree:add(attr_flags_raw, SXP_ATTRIBUTE_FLAGS_DESC[SXP_ATTRIBUTE_FLAGS[idx]])
        end
    end
    return offset + 1, attr_flags
end


function read_attr_flags(flagBits)
    local attr_flags = {
        bit.band(flagBits, 0x80) > 0, -- ["O"]
        bit.band(flagBits, 0x40) > 0, -- ["N"]
        bit.band(flagBits, 0x20) > 0, -- ["P"]
        bit.band(flagBits, 0x10) > 0, -- ["C"]
        bit.band(flagBits, 0x08) > 0 -- ["E"]
    }

    -- ignore extended-flag bit if not compact
    if not attr_flags[4] then
        attr_flags[5] = false
    end

    return attr_flags
end


function attr_flags_tostring(flags)
    local summary = ""
    local bits = ""
    for idx, A in pairs(SXP_ATTRIBUTE_FLAGS) do
        if flags[idx] then
            summary = summary .. A
            bits = bits .. "1"
        else
            summary = summary .. A:lower()
            bits = bits .. "0"
        end
    end
    return bits .. "... -> " .. summary
end


function process_attribute_header(payloadBuffer, subtree, offset)
    local attr_tree = subtree:add(payloadBuffer(offset, 1), "in progress..")
    offset, flags = process_flags(payloadBuffer, attr_tree, offset)

    local attr_length = nil
    local attr_type = nil
    local attr_description = nil
    local attr_start = offset
    local attr_type_value = nil

    if flags[SXP_ATTRIBUTE_FLAG_NAMES["C"]] then
        attr_type = payloadBuffer(offset, 1)
        attr_type_value = attr_type:uint()
        offset = offset + 1
        local attr_type_name = SXP_ATTRIBUTE_TYPES[attr_type_value]
        if attr_type_name == nil then
            attr_type_name = "N/A"
        end
        attr_description = attr_type_name .. " [" .. attr_type_value .. "]"

        if flags[SXP_ATTRIBUTE_FLAGS["E"]] then
            -- extended structure
            attr_length = payloadBuffer(offset, 2)
            offset = offset + 2
        else
            -- compact structure
            attr_length = payloadBuffer(offset, 1)
            offset = offset + 1
        end
    else
        -- non-compact structure
        -- TBD ... 4b prefix - tail of flag byte
        attr_type = payloadBuffer(offset, 3)
        attr_type_value = attr_type:uint()
        offset = offset + 3
        local attr_type_name = SXP_ATTRIBUTE_TYPES[attr_type_value]
        attr_description = attr_type_name .. " [" .. attr_type_value .. "]"
        attr_length = payloadBuffer(offset, 4)
        offset = offset + 4
    end


    local attr_length_value = attr_length:uint()
    attr_tree:set_text("attribute: " .. attr_description)
    attr_tree:set_len(attr_length_value + (offset - attr_start) + 1)
    attr_tree:add(attr_type, "type: " .. attr_description)
    attr_tree:add(attr_length, "length [" .. attr_length_value .. "]")

    local attr_value_raw = payloadBuffer(offset, attr_length_value)
    offset = offset + attr_length_value

    return attr_tree, attr_type_value, attr_value_raw, offset
end
