# frozen_string_literal: true

# Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0, or
# GNU General Public License version 2, or
# GNU Lesser General Public License version 2.1.

module Truffle
  module EncodingOperations
    def self.dummy_encoding(name)
      new_encoding, index = Truffle.invoke_primitive :dummy_encoding, name
      ::Encoding::EncodingMap[name.upcase.to_sym] = [nil, index]
      [new_encoding, index]
    end
  end
end
