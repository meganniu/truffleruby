#!/usr/bin/env ruby

#
# Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
# This code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0, or
# GNU General Public License version 2, or
# GNU Lesser General Public License version 2.1.
#

TRUFFLERUBY_DIR = File.expand_path('../..', __FILE__)
Dir.chdir(TRUFFLERUBY_DIR)

require 'erb'

types = {
  'void*' => '0', 'void' => nil,
  'bool' => 'false', 'int' => '0', 'long' => '0', 'char' => "'0'",
  'int8_t' => '0', 'int16_t' => '0', 'int32_t' => '0', 'int64_t' => '0',
  'uint8_t' => '0', 'uint16_t' => '0', 'uint32_t' => '0', 'uint64_t' => '0',
  'float' => '0.0', 'double' => '0.0',
  'constchar*' => '""',
}

methods = []

lines = IO.readlines('lib/cext/include/sulong/truffle.h') + IO.readlines('lib/cext/include/sulong/polyglot.h')
lines.each do |line|
  # Ignore functions only defined for documentation
  break if line.start_with?('#ifdef DOXYGEN')
  next if line.start_with?('//') || line.start_with?(' *')

  match = /^(\S.+?)\b(truffle|polyglot|__polyglot)(.+)\)(?=;)/.match(line)
  if match
    signature, return_type = match[0], match[1]
    return_value = types.fetch(return_type.gsub(' ', '')) do
      raise "unknown type: `#{return_type}` for line `#{line}`"
    end
    methods << [signature, return_value]
  end
end

File.write('src/main/c/sulongmock/sulongmock.c', ERB.new(<<TRC).result)
/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 *
 */

// This file is automatically generated by ruby tool/generate-sulongmock.rb

#include <stdio.h>
#include <stdint.h>
#include <sulong/polyglot.h>
#include <sulong/truffle.h>

void rb_tr_mock() {
  fprintf(stderr, "Warning: Mock method called in sulongmock\\n");
  abort();
}
<% methods.each do |signature, return_value| %>
<%= signature %> {
  rb_tr_mock();<% if return_value %>
  return <%= return_value %>;<% end %>
}
<% end %>
TRC
