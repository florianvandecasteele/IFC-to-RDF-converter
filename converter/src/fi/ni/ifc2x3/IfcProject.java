package fi.ni.ifc2x3;
import java.util.List;

import fi.ni.IfcSet;

/*
 * IFC Java class
The MIT License (MIT)

Copyright (c) 2014 Jyrki Oraskari

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

public class IfcProject extends IfcObject 
{
 // The property attributes
 String longName;
 String phase;
 List<IfcRepresentationContext> representationContexts = new IfcSet<IfcRepresentationContext>();
 IfcUnitAssignment   unitsInContext;


 // Getters and setters of properties

 public String getLongName() {
   return longName;
 }
 public void setLongName(String value){
   this.longName=value;

 }

 public String getPhase() {
   return phase;
 }
 public void setPhase(String value){
   this.phase=value;

 }

 public List<IfcRepresentationContext> getRepresentationContexts() {
   return representationContexts;

 }
 public void setRepresentationContexts(IfcRepresentationContext value){
   this.representationContexts.add(value);

 }

 public IfcUnitAssignment getUnitsInContext() {
   return unitsInContext;

 }
 public void setUnitsInContext(IfcUnitAssignment value){
   this.unitsInContext=value;

 }

}
