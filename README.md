# BearMaps

## Berkeley CS61B Project 3 (inspired by Google Maps) 

This is a web mapping application of berkeley.

I developed the backend feautures and implemented Rasterer, GraphDB, GraphBuildingHandler, and Router using Java, Apache Maven, AWS.

Files:
- Rasterer: Renders map images given a user's requested area and degree of zoom.
- GraphDB: Graph representation of the contents of Berkeley OSM.
- GraphBuildingHandler: Handler used by SAX parser to parse Nodes and Ways from Berkeley OSM file.
- Route: Uses A* search algorithm to find the shortest path between two points in Berkeley.

Reference:
https://cs61bl.org/su18/projects/bearmaps/

