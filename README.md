
# A Java Terminal UI Component Library.

Text UI library written by David Balme (davidbalme@gmail.com)

Purpose is to be able to render UI objects on a linux based terminals.

##### Background:

Originally I wanted to make a very simple set of utility classes to help with VT-100 terminal Control Codes manipulation
for a command-line interface.

To say I went a little overboard ..  Well .. that just under states it!  :)

##### Some notes:

Library is pure java.

To get screen characteristics (size) the library ocasionnally shells out and calls tputs.

The JVM and tputs is pretty much the extent of its dependency to the outside world.

It doesn't even have an external dependencies to 3rd-party libraries!!

When used for user input, the library is event based.

#### Diagramming!


Inspired by the [python library drawille](https://github.com/asciimoo/drawille)

This library can also be used to construct ascii images ( boxes, triangles circles, charts )..
These can be output to the terminal or output into a string.

The library is still in a beta form but is robust enough for small production applications.


# Code Examples:  

All code examples are located in the java package _dtb.terminalIO.examples_ ..

### Basic input controls 

code in : dtb.terminalIO.examples.TextBoxTest

![dtb.terminalIO.examples.TextBoxTest](./docs/img/TextBoxTestFormsExample.png)

### MultiLineTextInput

code in : dtb.terminalIO.examples.MultiLineTextInputTest

![dtb.terminalIO.examples.MultiLineTextInputTest](./docs/img/MultiLineEdit.png)

### ListBox

code in : dtb.terminalIO.examples.ListBoxTest

![dtb.terminalIO.examples.ListBoxTest](./docs/img/ListBoxTest.png)


### TabPanel

code in : dtb.terminalIO.examples.TabPanelTest

![dtb.terminalIO.examples.TabPanelTest](./docs/img/TabPanelExample.png)

#  This library does primitive graphics -- using Braille Characters !!!

### Examples

#### Braille Example

code in : dtb.terminalIO.examples.GridBrailleTest

![dtb.terminalIO.examples.GridBrailleTest](./docs/img/GridBrailleTest.png)


#### LineChart

code in : dtb.terminalIO.examples.LineChartTest

![dtb.terminalIO.examples.LineChartTest](./docs/img/LineChartTest.png)


### Donut Chart


code in : dtb.terminalIO.examples.DonutChartTest

![dtb.terminalIO.examples.DonutChartTest](./docs/img/DonutChartTest.png)

## Some non-braille graphics Bar-chart / Sparkline ..

#### BarChart

code in : dtb.terminalIO.examples.BarChartTest

![dtb.terminalIO.examples.BarChartTest](./docs/img/BarChartTest.png)

#### SparkLine

code in : dtb.terminalIO.examples.SparklineTest

![dtb.terminalIO.examples.SparklineTest](./docs/img/SparkLineTest.png)


#### Want to output UI to a string instead of to the terminal?

Example :

Left shows with Braille Characters - right shows with Braille turned off and mapping to simple ascii chars.


code in : dtb.terminalIO.examples.StringOutputTest

![dtb.terminalIO.examples.StringOutputTest](./docs/img/StringOutputTest.png)

