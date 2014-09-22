package org.buildingsmart.IFC4_ADD1;
import org.buildingsmart.IFC4_ADD1.interfaces.*;
import org.buildingsmart.*;
import java.util.*;

/*
 * IFC Java class
The MIT License (MIT)

 * @author Jyrki Oraskari
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

public class IfcMaterialProfileSetUsage extends IfcMaterialUsageDefinition 
{
 // The property attributes
 IfcMaterialProfileSet   forProfileSet;
 Long cardinalPoint;
 Double referenceExtent;


 // Getters and setters of properties

 public IfcMaterialProfileSet getForProfileSet() {
   return forProfileSet;

 }
 public void setForProfileSet(IfcMaterialProfileSet value){
   this.forProfileSet=value;

 }

 public Long getCardinalPoint() {
   return cardinalPoint;
 }
 public void setCardinalPoint(String txt){
   Long value = i.toLong(txt);
   this.cardinalPoint=value;

 }

 public Double getReferenceExtent() {
   return referenceExtent;
 }
 public void setReferenceExtent(String txt){
   Double value = i.toDouble(txt);
   this.referenceExtent=value;

 }

}
