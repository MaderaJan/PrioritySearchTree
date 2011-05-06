/******************************************************************************
*                       Copyright (c) 2011 - 2012 by                          *
*                               Simon Pratt                                   *
*                         (All rights reserved)                               *
*******************************************************************************
*                                                                             *
* FILE:    ArrayPST.java                                                      *
*                                                                             *
* MODULE:  Priority Search Tree                                               *
*                                                                             *
* NOTES:   A priority search tree is a tree data structure which stores       *
*          a set of coordinates in sorted order.  The root element of         *
*          the tree is the point with highest Y value.  The rest of           *
*          the points are divided into two sets, one set having               *
*          smaller x values than the median, the other having higher.         *
*                                                                             *
*          See README for more information.                                   *
*                                                                             *
*          See LICENSE for license information.                               *
*                                                                             *
******************************************************************************/

import java.awt.geom.*;
import java.util.*;

public class ArrayPST implements PrioritySearchTree {
    ArrayPSTNode[] heap;

/******************************************************************************
* The worst case for space is when there are 2^m nodes, for some m.           *
* In which case, O(2^(logn) - 1) extra space is allocated.                    *
******************************************************************************/
    public ArrayPST(ArrayList<PSTPoint> points) {
	if(points == null) return;
	Collections.sort(points); // Sort by y-coordinate in increasing order
	this.heap = new ArrayPSTNode[heapSize(treeHeight(points.size()))];
	buildTree(0,points);
    }
/******************************************************************************
* Given a root index and a list of valid points P ordered by                  *
* y-coordinate in increasing order, determines a median which bisects         *
* the remaining points, then builds:                                          *
*                                                                             *
*   root: point with lowest y-value                                           *
*   left child:  {p ∈ (P - root) | p.x <= medianX}                            *
*   right child: {p ∈ (P - root) | p.x >  medianX}                            *
*                                                                             *
* Note: points are also assumed to have distinct coordinates, i.e. no         *
*       two points have the same x coordinate and no two points have          *
*       the same y coordinate.                                                *
*                                                                             *
*       While this may seem unrealistic, we can convert any indistinct        *
*       coordinates by replacing all real coordinates with distinct           *
*       coordinates from the composite-number space without any loss          *
*       of generality.  See: Computational Geometry: Applications and         *
*       Algorithms, de Berg et al.  Section 5.5.                              *
*                                                                             *
******************************************************************************/
    private void buildTree(int rootIndex, ArrayList<PSTPoint> points) {
	if(points == null || points.size() < 1) return;
	// Since points are ordered by y increasing, smallest is first
	PSTPoint rootPoint = points.remove(0);
	// Find median X value
	//  - uses average X value of non-root points
	double sumX = 0.0d;
	for(PSTPoint p : points)
	    sumX += p.getX();
	double medianX = sumX/points.size();
	// Set the root node
	heap[rootIndex] = new ArrayPSTNode(rootPoint);
	// Bisect the non-root points into two arrays above and below the median
	ArrayList<PSTPoint> upperPoints = new ArrayList<PSTPoint>();
	ArrayList<PSTPoint> lowerPoints = new ArrayList<PSTPoint>();
	for(PSTPoint p : points) {
	    // note: if p.x is equal to median, it will be added to left child
	    if(p.getX() <= medianX) lowerPoints.add(p);
	    else upperPoints.add(p);
	}
	if(lowerPoints.size() > 0)
	    buildTree(indexOfLeftChild(rootIndex),lowerPoints);
	if(upperPoints.size() > 0)
	    buildTree(indexOfRightChild(rootIndex),upperPoints);
    }
/******************************************************************************
*                                                                             *
* Find all points within the region bounded by (minX,minY) and (maxX,maxY)    *
*                                                                             *
*              +--------+ (maxX,maxY)                                         *
*              |        |                                                     *
*              |        |                                                     *
*              |        |                                                     *
*  (minX,minY) +--------+                                                     *
*                                                                             *
* Assumes maxX > minX and maxY > minY.                                        *
* Choose minX,minY,maxX,maxY appropriately.                                   *
*                                                                             *
******************************************************************************/
    public ArrayList<PSTPoint> findAllPointsWithin(double minX, 
						   double maxX, double maxY)
	throws EmptyTreeException {
	return findAllPointsWithin(minX,maxX,maxY,
				   new ArrayList<PSTPoint>(),0);
    }
    // Note that as maxY and maxX approach positive infinity and
    // minX approaches negative infinity, this search visits more nodes.
    // In the worst case, all nodes are visited.
    private ArrayList<PSTPoint> findAllPointsWithin(double minX,
						    double maxX, double maxY,
						    ArrayList<PSTPoint> list,
						    int index)
	throws EmptyTreeException {
	ArrayPSTNode node = heap[index];
	if(node == null) return list;
	if(node.getY() <= maxY) {
	    double nodeX = node.getX();
	    if(nodeX >= minX && nodeX <= maxX) { 
		list.add(node.getPoint());
	    }
	    if(isValidNode(indexOfLeftChild(index))) {
		double nodeR = maxX(index);
		// nodeR >= points in left tree >= minX
		if(nodeR >= minX)
		    findAllPointsWithin(minX,maxX,maxY,list,
					indexOfLeftChild(index));
		// nodeR < points in right tree <= maxX
		if(nodeR < maxX) 
		    findAllPointsWithin(minX,maxX,maxY,list,
					indexOfRightChild(index));
	    }
	}
	return list;
    }
/******************************************************************************
* Other query functions                                                       *
******************************************************************************/
    public double minYinRange(double minX, double maxX, double maxY)
	throws NoPointsInRangeException {
	double min = minYinRange(minX,maxX,maxY,0);
	if(min < Double.POSITIVE_INFINITY) return min;
	throw new NoPointsInRangeException();
    }
    private double minYinRange(double minX, double maxX,double maxY, int index) {
	ArrayPSTNode node = heap[index];
	if(node == null || node.getY() > maxY) return Double.POSITIVE_INFINITY;
	double nodeX = node.getX();
	if(nodeX >= minX && nodeX <= maxX) return node.getY();
	if(isValidNode(indexOfLeftChild(index))) {
	    double nodeR = maxX(indexOfLeftChild(index));
	    // nodeR >= points in left tree >= minX
	    if(nodeR >= minX && 
	       nodeR < maxX && isValidNode(indexOfRightChild(index))) {
		double minLeft = minYinRange(minX,maxX,maxY,indexOfLeftChild(index));
		double minRight = minYinRange(minX,maxX,maxY,indexOfRightChild(index));
		return (minLeft < minRight ? minLeft : minRight);
	    } else if(nodeR >= minX) {
		return minYinRange(minX,maxX,maxY,indexOfLeftChild(index));
	    } else if(nodeR < maxX && isValidNode(indexOfRightChild(index))) {
		return minYinRange(minX,maxX,maxY,indexOfRightChild(index));
	    }
	}
	return Double.POSITIVE_INFINITY;
    }
    public double minXinRange(double minX, double maxX, double maxY)
	throws NoPointsInRangeException {
	double min = minXinRange(minX,maxX,maxY,0);
	if(min < Double.POSITIVE_INFINITY) return min;
	throw new NoPointsInRangeException();
    }
    private double minXinRange(double minX, double maxX, double maxY, int index) {
	ArrayPSTNode node = heap[index];
	if(node == null || node.getY() > maxY)
	    return Double.POSITIVE_INFINITY;
	double min = Double.POSITIVE_INFINITY;
	double nodeX = node.getX();
	if(minX <= nodeX && nodeX <= maxX)
	    min = nodeX;
	if(isValidNode(indexOfLeftChild(index))) {
	    double nodeR = maxX(indexOfLeftChild(index));
	    if(nodeR >= minX) {
		double minLeft = minXinRange(minX,maxX,maxY,indexOfLeftChild(index));
		if(minLeft < min) min = minLeft;
	    }
	    if(nodeR < maxX && isValidNode(indexOfRightChild(index))) {
		double minRight = minXinRange(minX,maxX,maxY,indexOfRightChild(index));
		if(minRight < min) min = minRight;
	    }
	}
	return min;
    }
    public double maxXinRange(double minX, double maxX, double maxY)
	throws NoPointsInRangeException {
	double max = maxXinRange(minX,maxX,maxY,0);
	if(max > Double.NEGATIVE_INFINITY) return max;
	throw new NoPointsInRangeException();
    }
    private double maxXinRange(double minX, double maxX, double maxY, int index) {
	ArrayPSTNode node = heap[index];
	if(node == null || node.getY() > maxY)
	    return Double.NEGATIVE_INFINITY;
	double max = Double.NEGATIVE_INFINITY;
	double nodeX = node.getX();
	if(minX <= nodeX && nodeX <= maxX)
	    max = nodeX;
	if(isValidNode(indexOfLeftChild(index))) {
	    double nodeR = maxX(indexOfLeftChild(index));
	    if(nodeR >= minX) {
		double maxLeft = maxXinRange(minX,maxX,maxY,indexOfLeftChild(index));
		if(maxLeft > max) max = maxLeft;
	    }
	    if(nodeR < maxX && isValidNode(indexOfRightChild(index))) {
		double maxRight = maxXinRange(minX,maxX,maxY,indexOfRightChild(index));
		if(maxRight > max) max = maxRight;
	    }
	}
	return max;
    }
    public double maxYinRange(double minX, double maxX, double maxY)
	throws NoPointsInRangeException {
	double max = maxYinRange(minX,maxX,maxY,0);
	if(max > Double.NEGATIVE_INFINITY) return max;
	throw new NoPointsInRangeException();
    }
    private double maxYinRange(double minX, double maxX, double maxY, int index) {
	ArrayPSTNode node = heap[index];
	if(node == null || node.getY() > maxY)
	    return Double.NEGATIVE_INFINITY;
	double max = Double.NEGATIVE_INFINITY;
	double nodeX = node.getX();
	if(minX <= nodeX && nodeX <= maxX)
	    max = node.getY();
	if(isValidNode(indexOfLeftChild(index))) {
	    double nodeR = maxX(indexOfLeftChild(index));
	    if(nodeR >= minX) {
		double maxLeft = maxYinRange(minX,maxX,maxY,indexOfLeftChild(index));
		if(maxLeft > max) max = maxLeft;
	    }
	    if(nodeR < maxX && isValidNode(indexOfRightChild(index))) {
		double maxRight = maxYinRange(minX,maxX,maxY,indexOfRightChild(index));
		if(maxRight > max) max = maxRight;
	    }
	}
	return max;
    }
	
/******************************************************************************
* Whole-tree query functions                                                  *
******************************************************************************/
    public double minX() throws EmptyTreeException {
	int index = 0;
	if(heap[index] == null) throw new EmptyTreeException();
	double min = heap[index].getX();
	while(isValidNode(indexOfLeftChild(index))) {
	    index = indexOfLeftChild(index);
	    if(heap[index].getX() < min)
		min = heap[index].getX();
	}
	return min;
    }
    public double maxX() {
	return maxX(0);
    }
    private double maxX(int index) {
	double max = heap[index].getX();
	while(isValidNode(indexOfRightChild(index))) {
	    index = indexOfRightChild(index);
	    if(heap[index].getX() > max)
		max = heap[index].getX();
	}
	// Since a leaf without a sibling is always left
	// we have to check the last left child just in case
	if(isValidNode(indexOfLeftChild(index)) &&
	   heap[indexOfLeftChild(index)].getX() > max)
	    max = heap[indexOfLeftChild(index)].getX();
	return max;
    }
    public double minY() throws EmptyTreeException {
	if(heap[0] == null) throw new EmptyTreeException();
	return heap[0].getY();
    }
    public double maxY() throws EmptyTreeException {
	if(heap[0] == null) throw new EmptyTreeException();
	return maxY(0);
    }
    private double maxY(int index) {
	double max = heap[index].getY();
	if(isValidNode(indexOfRightChild(index)) &&
	   isValidNode(indexOfLeftChild(index))) {
		double maxLeft = maxY(indexOfLeftChild(index));
		double maxRight = maxY(indexOfRightChild(index));
		if(maxLeft > maxRight) max = maxLeft;
		else max = maxRight;
	} else if(isValidNode(indexOfRightChild(index))) {
		max = maxY(indexOfRightChild(index));
	} else if(isValidNode(indexOfLeftChild(index))) {
		max = maxY(indexOfLeftChild(index));
	}
	return max;
    }
/******************************************************************************
* Utility Functions                                                           *
******************************************************************************/
    private boolean isValidNode(int index) {
	return index < heap.length && heap[index] != null;
    }
    // height of a balanced tree with n elements
    private static int treeHeight(int n) {
	return doubleToInt(Math.ceil(Math.log(n+1)/Math.log(2)));
    }
    // max number of heap nodes in a tree of given height
    private static int heapSize(int height) {
	return doubleToInt(Math.pow(2, height)-1);
    }
    // width of a tree at a given depth
    private static int width(int depth) {
	return doubleToInt(Math.pow(2,depth-1));
    }
    // amount of unused space allocated for a given number of nodes
    private static int waste(int n) {
	int height = treeHeight(n);
	return (width(height) - (n - heapSize(height-1)));
    }
    private static int indexOfLeftChild(int rootIndex) {
	return (2*rootIndex)+1;
    }
    private static int indexOfRightChild(int rootIndex) {
	return (2*rootIndex)+2;
    }
    private static int doubleToInt(double d) {
	return (new Double(d)).intValue();
    }
    private static void printList(ArrayList<PSTPoint> points) {
	for(PSTPoint p : points) System.out.print(p + " ");
	System.out.println();
    }
    private static void report(int n) {
	System.out.println("Nodes: " + n);
	int height = treeHeight(n);
	System.out.println("Tree depth: " + height);
	int heapSize = heapSize(height);
	System.out.println("Heap size: " + heapSize);
	System.out.println("Width at max depth: " + width(height));
	System.out.println("Unused nodes: " + (heapSize - n));
    }
/******************************************************************************
* Testing                                                                     *
******************************************************************************/  
    public static void main(String[] args)
	throws EmptyTreeException, NoPointsInRangeException {
	ArrayList<PSTPoint> testPoints = new ArrayList<PSTPoint>();
	double MAX_Y = 2500000d;
	for(double i = 0; i < MAX_Y ; i++) {
	    testPoints.add(new PSTPoint(MAX_Y-i,i));
	    testPoints.add(new PSTPoint(-i,-MAX_Y+i));
	}

	// Build tree recursive
	System.out.println("Building tree with " +
			   (2*doubleToInt(MAX_Y)) + " nodes recursively...");
	StopWatch sw = new StopWatch();
	ArrayPST pst = new ArrayPST(testPoints);
	long time = sw.stop();
	System.out.println("Took: " + time);

	// Test time
	testTime(pst,MAX_Y);
    }
    private static void testTime(ArrayPST pst, double MAX_Y) 
	throws EmptyTreeException, NoPointsInRangeException {
	// Find all points in range
	System.out.println("Finding all points in range...");
	StopWatch sw = new StopWatch();
	ArrayList<PSTPoint> testPoints = pst.findAllPointsWithin(-MAX_Y,MAX_Y,MAX_Y);
	long time = sw.stop();
	System.out.println("Took: " + time);

	// Find max/min x/y in range
	System.out.println("Finding max/min x/y in range...");
	double result;
	sw = new StopWatch();
	result = pst.minYinRange(-MAX_Y,MAX_Y,MAX_Y);
	result = pst.minXinRange(-MAX_Y,MAX_Y,MAX_Y);
	result = pst.maxXinRange(-MAX_Y,MAX_Y,MAX_Y);
	result = pst.maxYinRange(-MAX_Y,MAX_Y,MAX_Y);
	time = sw.stop();
	System.out.println("Took: " + time);
    }
}