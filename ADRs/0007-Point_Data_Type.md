# Point Datatype

## Status
ACCEPTED

Proposed by: Paul Dubs (05/06/2020)

Discussed with: Alex Black

## Context
There are multiple ways one can represent a point with the existing data types. However, all of those ways can be classified as a workaround. A bounding box with zero width and height, a list of doubles, or an NDArray all don't communicate the intent of their contents well.

Because communicating intent is important when building maintainable systems, a Point data type was suggested.

## Decision
We create a Point datatype that is explicitly meant to be used for point based data. The Point datatype can represent points with any number of dimensions. It is **not** limited to just 2 or 3 dimensional points.

In order to make conversion between bounding boxes and points easier, points like bounding boxes will also provide optional "label" and "probability" fields.

To access a specific dimension of a point, a direct `.get(dimension)` method is provided. Because we anticipate that 2 and 3 dimensional points are going to be used very often, the typical x, y, z based notation will also be allowed trough the use of `.x()`, `.y()` and `.z()` methods which internally call `.get(0)`, `.get(1)` or `.get(2)` respectively.

The value of a point will *usually* fall between 0 and 1, i.e. will be a relative measure. This is especially useful when used with other data types like image. However, it is not required to be within this range meaning absolute values are also allowed - interpreting the meaning of a point value is up to the user.

The point data type is going to be implemented with an interface / implementation split, however only a single implementation that can take n-dimensional points will be used. If this should ever become a problem, we should be able to provide specialized implementations without breaking existing code. 
 
## Consequences 

### Advantages
* A point can semantically be represented using the point datatype
  
### Disadvantages
* We add another datatype instead of using what exists already 

## Discussion
We have discussed different ways of implementing the point data type. The discussed options included just a single implementation without a specific interface, an interface with multiple implementations (2d, 3d, n-d), and the ultimately chosen single implementation with an interface definition.

The benefits of this approach are:
* We can provide a fixed interface that can stay intact even when additional implementations are added
* We don't risk having some unmaintained / seldom used / unused point types because specific implementations are used most often
