-- sxp4: ERROR message handling

require "sxp4-common"

function handle_error_msg(payloadBuffer, subtree)
    -- handle error code
    local error_flag = payloadBuffer(0, 1)
    local error_flag_bits = bit.tobit(error_flag:uint())
    local is_extended = bit.band(error_flag_bits, 0x80) > 0
    subtree:add(error_flag, "is-extended: " .. tostring(is_extended))

    if is_extended then
        local error_code_value = bit.band(error_flag_bits, 0x7f)
        local error_sub_code = payloadBuffer(1, 1)
        subtree:add(error_flag, "error-code : " .. SXP_ERROR_CODES[error_code_value] .. " [" .. error_code_value .. "]")
        local error_sub_code_value = error_sub_code:uint()
        subtree:add(error_sub_code, "error-sub-code : " .. SXP_ERROR_SUB_CODES[error_sub_code_value] .. " [" .. error_sub_code_value .. "]")
        local error_data = payloadBuffer(2)
        subtree:add(error_data, "error-data : " .. error_data:string())
    else
        local error_code = payloadBuffer(2, 2)
        local error_code_value = error_code:uint()
        subtree:add(error_code, "error-code (legacy) : " .. SXP_ERROR_CODES[error_code_value] .. " [" .. error_code_value .. "]")
    end
end

