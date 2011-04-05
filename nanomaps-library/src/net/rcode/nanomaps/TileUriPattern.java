package net.rcode.nanomaps;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores a pattern for assembling a URI from a CartesianTileKey.
 * Pattern strings can contain the following parameters:
 * <ul>
 * <li>${level} - The integral level
 * <li>${tilex} - The integral tile x value
 * <li>${tiley} - The integral tile y value
 * <li>${quadkey} - The Microsoft quadkey value
 * <li>${modulo:1,2,3} - Picks one of the comma-delimitted arguments based on a stable hash of the x/y/level
 * </ul>
 * <p>
 * Sample patterns:
 * <ul>
 * <li>http://otile${modulo:1,2,3}.mqcdn.com/tiles/1.0.0/osm/${level}/${tileX}/${tileY}.png
 * <li>http://${modulo:a,b,c}.tile.openstreetmap.org/${level}/${tileX}/${tileY}.png
 * <li>http://h0.ortho.tiles.virtualearth.net/tiles/h${quadkey}.jpeg?g=131
 * <li>http://ecn.t${modulo:1,2,3}.tiles.virtualearth.net/tiles/r${quadkey}?g=603&mkt=en-us&lbl=l1&stl=h&shading=hill&n=z
 * </ul>
 * 
 * @author stella
 *
 */
public class TileUriPattern {
	static final int OPCODE_LITERAL=0;
	static final int OPCODE_LEVEL=1;
	static final int OPCODE_TILEX=2;
	static final int OPCODE_TILEY=3;
	static final int OPCODE_QUADKEY=4;
	static final int OPCODE_MODULO=5;
	
	static class Part {
		public int opcode;
		public Object arg;
	}
	
	static final Pattern PARAM_PATTERN=Pattern.compile("\\$\\{([A-Za-z]+)(\\:([^\\}]*))?\\}");
	static final Pattern COMMA_PATTERN=Pattern.compile("\\,");
	Part[] parts;
	int length;
	
	public TileUriPattern(String pattern) throws IllegalArgumentException {
		ArrayList<Part> parts=new ArrayList<Part>();
		Matcher m=PARAM_PATTERN.matcher(pattern);
		int index=0;
		Part part;
		while (m.find()) {
			if (m.start() > index) {
				part=new Part();
				part.opcode=OPCODE_LITERAL;
				part.arg=pattern.substring(index, m.start());
				length+=m.start()-index;
				parts.add(part);
			}
			
			String token=m.group(1);
			String operand=m.group(3);
			part=new Part();
			if ("tilex".equalsIgnoreCase(token)) {
				part.opcode=OPCODE_TILEX;
				length+=8;
			} else if ("tiley".equalsIgnoreCase(token)) {
				part.opcode=OPCODE_TILEY;
				length+=8;
			} else if ("level".equalsIgnoreCase(token)) {
				part.opcode=OPCODE_LEVEL;
				length+=4;
			} else if ("quadkey".equalsIgnoreCase(token)) {
				part.opcode=OPCODE_QUADKEY;
				length+=8;
			} else if ("modulo".equalsIgnoreCase(token)) {
				part.opcode=OPCODE_MODULO;
				part.arg=COMMA_PATTERN.split(operand);
				length+=operand.length();
			} else {
				throw new IllegalArgumentException("Unrecognized URI token " + token);
			}
			parts.add(part);
			
			index=m.end();
		}
		
		if (index<pattern.length()) {
			part=new Part();
			part.opcode=OPCODE_LITERAL;
			part.arg=pattern.substring(index, pattern.length());
			length+=pattern.length()-index;
			parts.add(part);
		}
		
		this.parts=parts.toArray(new Part[parts.size()]);
	}
	
	public CharSequence uriFor(TileKey tk) {
		CartesianTileKey ctk=(CartesianTileKey) tk;
		StringBuilder ret=new StringBuilder(length);
		for (int i=0; i<parts.length; i++) {
			Part part=parts[i];
			switch (part.opcode) {
			case OPCODE_LITERAL:
				ret.append((String)part.arg);
				break;
			case OPCODE_LEVEL:
				ret.append(ctk.level);
				break;
			case OPCODE_TILEX:
				ret.append(ctk.tileX);
				break;
			case OPCODE_TILEY:
				ret.append(ctk.tileY);
				break;
			case OPCODE_QUADKEY:
				generateQuadKey(ret, ctk);
				break;
			case OPCODE_MODULO:
				String[] options=(String[]) part.arg;
				int h=Math.abs(ctk.tileX ^ ctk.tileY ^ ctk.level) % options.length;
				ret.append(options[h]);
				break;
			}
		}
		return ret;
	}

	private void generateQuadKey(StringBuilder accum, CartesianTileKey ctk) {
		int i, mask, value;
		for (i=ctk.level; i>0; i--) {
			value=48;
			mask=1<<(i-1);
			if ((ctk.tileX&mask)!=0) value++;
			if ((ctk.tileY&mask)!=0) value+=2;
			accum.append((char)value);
		}
	}
}
