package com.cds.hiro.x12.structures

import groovy.transform.CompileStatic

abstract class Element {
  protected static <T> T valueOf(String input, Class<T> clazz) {
    if (Enum.isAssignableFrom(clazz)) {
      clazz.byCode(input)
    } else if (clazz == String) {
      input as T
    } else if (clazz == Integer) {
      Integer.parseInt(input) as T
    } else if (clazz == Double) {
      Double.parseDouble(input) as T
    } else {
      throw new Exception("Unhandled type - ${clazz}")
    }
  }
}

@CompileStatic
abstract class Composite extends Element {
  abstract void parse(List<String> input)
  abstract List<String> toTokens()

}

@CompileStatic
abstract class BlockElement extends Element {
}

@CompileStatic
abstract class Message extends BlockElement {
  abstract List<List<List<List<String>>>> toTokens(int indent)
  abstract void parse(List<List<List<List<String>>>> input)
}

@CompileStatic
abstract class Loop extends BlockElement {
  abstract List<List<List<List<String>>>> toTokens(int indent)
  abstract void parse(List<List<List<List<String>>>> input)
}

@CompileStatic
abstract class Segment extends BlockElement {
  abstract void parse(List<List<List<String>>> input)
  abstract List<List<List<String>>> toTokens(int indent)

  protected static <T> T valueOf(List<List<String>> strings, Class<T> clazz) {
    def list = listOf(strings, clazz)
    list.size() > 0 ? list.first() : null
  }

  protected static <T> List<T> listOf(List<List<String>> strings, Class<T> clazz) {
    if (Composite.isAssignableFrom(clazz)) {
      strings.collect { array ->
        Composite instance1 = clazz.newInstance() as Composite
        instance1.
            with { instance ->
              instance.parse(array)
              instance
            } as T
      } as List<T>
    } else {
      strings.collect {valueOf(it[0], clazz)}
    }
  }
}
