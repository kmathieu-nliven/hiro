package com.cds.hiro.x12.structures

abstract class Element {}
abstract class BlockElement extends Element {}

abstract class Message extends BlockElement {
  abstract void parse(List<List<List<String[]>>> input)
}
abstract class Loop extends BlockElement {
  abstract void parse(List<List<List<String[]>>> input)
}
abstract class Segment extends BlockElement {
  abstract void parse(List<List<String[]>> input)
}
