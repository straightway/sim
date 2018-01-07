/****************************************************************************
Copyright 2016 github.com/straightway

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 ****************************************************************************/
package straightway.sim.net

import straightway.units.AmountOfData
import straightway.units.UnitValue
import straightway.units.byte
import straightway.units.get
import java.util.*

fun createMessage(size: UnitValue<Int, AmountOfData> = 100[byte]) =
    Message("Message(ID=${UUID.randomUUID()}", size)
