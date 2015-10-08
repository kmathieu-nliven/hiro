package com.cds.hiro.x12.structures

abstract class Element {
  abstract void parse(String input)
}
abstract class BlockElement extends Element {}
abstract class Message extends BlockElement {}
abstract class Segment extends BlockElement {}
abstract class Loop extends BlockElement {}
