package mpicbg.image;

public class StreamIterator
		extends StreamCursor
		implements Iterator, Localizable, LocalizableFactory< StreamIterator >
{	
	protected int i = 0;
	
	StreamIterator( Stream container )
	{
		super( container, null, new AccessDirect() );
	}
	
	final public boolean isInside(){ return i > -1 && i < container.getNumPixels(); }
	
	public void next(){ i += container.getPixelType().getNumChannels(); }
	public void prev(){ i -= container.getPixelType().getNumChannels(); }
	
	public float[] localize()
	{
		float[] l = new float[ container.getNumDim() ];
		localize( l );
		return l;
	}
	public void localize( float[] l )
	{
		int r = i / container.getPixelType().getNumChannels();
		for ( int d = 0; d < l.length; ++d )
		{
			l[ d ] = r % container.getDim( d );
			r /= container.getDim( d );
		}		
	}
	public void localize( int[] l )
	{
		int r = i / container.getPixelType().getNumChannels();
		
		for ( int d = 0; d < l.length; ++d )
		{
			l[ d ] = r % container.getDim( d );
			r /= container.getDim( d );
		}		
	}

	public IteratorByDimension toIteratableByDimension( )
	{
		int[] l = new int[ container.getNumDim() ];
		localize( l );
		return new StreamIteratorByDimension( container, l, accessStrategy );
	}

	public RandomAccess toRandomAccessible( )
	{
		int[] l = new int[ container.getNumDim() ];
		localize( l );
		return new StreamRandomAccess( container, l, accessStrategy );
	}

	public int getStreamIndex() { return i; }
}
