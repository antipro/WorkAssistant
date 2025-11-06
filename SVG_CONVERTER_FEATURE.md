# SVG to PNG Conversion Feature

## Overview
This feature provides utilities to detect and convert SVG clipboard content to PNG format for the WorkAssistant clipboard feature.

## Dependencies Added

### Apache Batik (Version 1.17)
Added to `pom.xml`:
```xml
<!-- Apache Batik for SVG to PNG conversion -->
<dependency>
    <groupId>org.apache.xmlgraphics</groupId>
    <artifactId>batik-transcoder</artifactId>
    <version>1.17</version>
</dependency>
<dependency>
    <groupId>org.apache.xmlgraphics</groupId>
    <artifactId>batik-codec</artifactId>
    <version>1.17</version>
</dependency>
<dependency>
    <groupId>org.apache.xmlgraphics</groupId>
    <artifactId>xmlgraphics-commons</artifactId>
    <version>2.9</version>
</dependency>
```

**Security**: All dependencies scanned with GitHub Advisory Database - no vulnerabilities found.

## SVGConverter Utility Class

Located at: `src/main/java/com/workassistant/util/SVGConverter.java`

### Features
- **MIME Type Detection**: Detects `image/svg+xml`, `text/svg`, and SVG content in `text/xml`
- **Content-Based Detection**: Inspects XML content for SVG markers
- **Automatic Dimension Extraction**: Extracts width/height from SVG, uses 800x600 defaults
- **Multiple Input Formats**: Accepts String, byte[], or InputStream
- **Headless-Compatible**: Works in server environments without AWT/GUI
- **Robust Error Handling**: Validates inputs and handles edge cases

### Usage Examples

#### 1. Check if content is SVG
```java
// Check MIME type
boolean isSVG = SVGConverter.isSVGMimeType("image/svg+xml"); // true
boolean isNotSVG = SVGConverter.isSVGMimeType("image/png"); // false

// Check content
String xmlContent = "<svg xmlns=\"http://www.w3.org/2000/svg\">...</svg>";
boolean hasSVG = SVGConverter.isSVGContent(xmlContent, "text/xml"); // true
```

#### 2. Convert SVG to PNG (automatic dimensions)
```java
String svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"200\">...</svg>";
String pngDataUri = SVGConverter.convertSVGToPNG(svgContent);
// Returns: "data:image/png;base64,iVBORw0KG..."
```

#### 3. Convert SVG to PNG (custom dimensions)
```java
String svgContent = "...";
String pngDataUri = SVGConverter.convertSVGToPNG(svgContent, 800, 600);
```

#### 4. Extract dimensions from SVG
```java
int[] dimensions = SVGConverter.extractSVGDimensions(svgContent);
int width = dimensions[0];  // 300
int height = dimensions[1]; // 200
```

### Integration with Clipboard Feature

To integrate with the existing clipboard feature in `ChatController`:

```java
// In clipboard processing method
ClipboardData clipboardData = request.getContent();

for (ClipboardData.ClipboardImage image : clipboardData.getImages()) {
    String imageData = image.getData();
    String mimeType = image.getType();
    
    // Check if it's SVG content
    if (SVGConverter.isSVGMimeType(mimeType) || 
        SVGConverter.isSVGContent(imageData, mimeType)) {
        
        try {
            // Convert SVG to PNG
            String pngDataUri = SVGConverter.convertSVGToPNG(imageData);
            
            // Update the image data and type
            image.setData(pngDataUri);
            image.setType("image/png");
            
            logger.info("Converted SVG to PNG for clipboard image");
            
        } catch (Exception e) {
            logger.error("Failed to convert SVG to PNG", e);
            // Continue with original SVG data
        }
    }
    
    // Continue with normal image processing...
}
```

### Supported SVG MIME Types

- `image/svg+xml` - Standard SVG MIME type
- `text/svg` - Alternative SVG MIME type
- `text/xml` - If content contains `<svg` tag
- `application/xml` - If content contains `<svg` tag

### Error Handling

The utility provides comprehensive error handling:

```java
try {
    String pngData = SVGConverter.convertSVGToPNG(svgContent);
} catch (IllegalArgumentException e) {
    // Invalid input (null, empty, or invalid dimensions)
} catch (TranscoderException e) {
    // SVG parsing/transcoding error
} catch (IOException e) {
    // I/O error during conversion
}
```

### Edge Cases Handled

1. **Null or Empty Content**: Throws `IllegalArgumentException`
2. **Invalid SVG**: Throws `TranscoderException` with details
3. **Missing Dimensions**: Uses defaults (800x600)
4. **Negative Dimensions**: Converts to positive values
5. **Dimensions with Units**: Strips units (px, pt, etc.)
6. **Case Insensitivity**: MIME types checked case-insensitively

## Testing

### Unit Tests
Located at: `src/test/java/com/workassistant/util/SVGConverterTest.java`

- **28 comprehensive tests** covering all functionality
- **All tests passing** ✓
- **No regressions** introduced

### Test Coverage
- MIME type detection (various formats)
- Content-based SVG detection
- SVG to PNG conversion (valid/invalid)
- Custom dimension handling
- Dimension extraction
- Error handling (null, empty, invalid)
- Multiple input formats

### Running Tests
```bash
mvn test -Dtest=SVGConverterTest
```

## Performance Considerations

- **Memory**: SVG conversion happens in-memory
- **CPU**: Transcoding is CPU-intensive but fast for typical SVGs
- **Thread Safety**: All methods are static and thread-safe
- **Async Processing**: Recommended to run conversions in async tasks (existing `aiExecutor`)

## Environment Compatibility

✓ **Headless Servers**: No AWT/GUI dependencies required  
✓ **Docker Containers**: Works in containerized environments  
✓ **Desktop Environments**: Fully compatible  
✓ **Java 17+**: Tested with Java 17

## Security

- **No Vulnerabilities**: All dependencies scanned with GitHub Advisory Database
- **CodeQL Analysis**: No security alerts found
- **Input Validation**: All inputs validated before processing
- **Error Handling**: Exceptions caught and logged appropriately

## Limitations

1. **Complex SVG Features**: Some advanced SVG features may not render perfectly
2. **External Resources**: SVG files referencing external resources may fail
3. **Very Large SVGs**: May consume significant memory during conversion
4. **Animation**: SVG animations are rendered as static images

## Future Enhancements

- Add configuration for default dimensions
- Support for SVG optimization before conversion
- Batch conversion support
- Progress callbacks for large conversions
- SVG sanitization for security

## References

- [Apache Batik Documentation](https://xmlgraphics.apache.org/batik/)
- [SVG Specification](https://www.w3.org/TR/SVG2/)
- [PNG Specification](https://www.w3.org/TR/PNG/)
