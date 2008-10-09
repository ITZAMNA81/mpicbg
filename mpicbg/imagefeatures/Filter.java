package mpicbg.imagefeatures;

/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de> and Stephan Preibisch <preibisch@mpi-cbg.de>
 *   
 * @version 0.2b
 */

public class Filter
{
	/**
	 * Return an unsigned integer that bounces in a ping pong manneris flipped in the range [0 ... mod - 1]
	 *
	 * @param a the value to be flipped
	 * @param range the size of the range
	 * @return a flipped in range like a ping pong ball
	 */
	final static public int flipInRange( int a, int mod )
	{
		final int p = 2 * mod;
		if ( a < 0 ) a = p + a % p;
		if ( a >= p ) a = a % p;
		if ( a >= mod ) a = mod - a % mod - 1;
		return a;
	}
	
	/**
	 * Fast floor log_2 of an unsigned integer value.
	 * 
	 * @param v unsigned integer
	 * @return floor log_2
	 */
	final static public int ldu( int v )
	{
	    int c = 0;
	    do
	    {
	    	v >>= 1;
	        ++c;
	    }
	    while ( v > 1 );
	    return c;
	}
	
    /**
     * Create a 1d-Gaussian kernel of appropriate size.
     *
     * @param sigma Standard deviation of the Gaussian kernel
     * @param normalize Normalize integral of the Gaussian kernel to 1 or not...
     * 
     * @return float[] Gaussian kernel of appropriate size
     */
    final static public float[] createGaussianKernel(
    		final float sigma,
    		final boolean normalize )
	{
		float[] kernel;

		if ( sigma <= 0 )
		{
			kernel = new float[ 3 ];
			kernel[ 1 ] = 1;
		}
		else
		{
			final int size = Math.max( 3, ( int ) ( 2 * ( int )( 3 * sigma + 0.5 ) + 1 ) );

			final float two_sq_sigma = 2 * sigma * sigma;
			kernel = new float[ size ];

			for ( int x = size / 2; x >= 0; --x )
			{
				final float val = ( float ) Math.exp( -( float ) ( x * x ) / two_sq_sigma );

				kernel[ size / 2 - x ] = val;
				kernel[ size / 2 + x ] = val;
			}
		}

		if ( normalize )
		{
			float sum = 0;
			for ( float value : kernel )
				sum += value;

			for ( int i = 0; i < kernel.length; i++ )
				kernel[ i ] /= sum;
		}

		return kernel;
	}

    /**
	 * Create a normalized 2d gaussian impulse with appropriate size with its
	 * center slightly moved away from the middle.
	 * 
	 */
	final static public FloatArray2D createGaussianKernelOffset(
			final float sigma,
			final float offset_x,
			final float offset_y,
			final boolean normalize )
	{
		final FloatArray2D kernel;
		if ( sigma == 0 )
		{
			kernel = new FloatArray2D( 3, 3 );
			kernel.data[ 4 ] = 1;
		}
		else
		{
			final int size = Math.max( 3, ( int ) ( 2 * Math.round( 3 * sigma ) + 1 ) );
			final float two_sq_sigma = 2 * sigma * sigma;
			// float normalization_factor = 1.0/(float)M_PI/two_sq_sigma;
			kernel = new FloatArray2D( size, size );
			for ( int x = size - 1; x >= 0; --x )
			{
				final float fx = ( float ) ( x - size / 2 );
				for ( int y = size - 1; y >= 0; --y )
				{
					final float fy = ( float ) ( y - size / 2 );
					final float val = ( float ) ( Math.exp( -( Math.pow( fx - offset_x, 2 ) + Math.pow( fy - offset_y, 2 ) ) / two_sq_sigma ) );
					kernel.set( val, x, y );
				}
			}
		}
		if ( normalize )
		{
			float sum = 0;
			for ( float value : kernel.data )
				sum += value;

			for ( int i = 0; i < kernel.data.length; i++ )
				kernel.data[ i ] /= sum;
		}
		return kernel;
	}

	final public static FloatArray2D[] createGradients( final FloatArray2D array )
	{
		final FloatArray2D[] gradients = new FloatArray2D[ 2 ];
		gradients[ 0 ] = new FloatArray2D( array.width, array.height );
		gradients[ 1 ] = new FloatArray2D( array.width, array.height );

		for ( int y = 0; y < array.height; ++y )
		{
			final int[] ro = new int[ 3 ];
			ro[ 0 ] = array.width * Math.max( 0, y - 1 );
			ro[ 1 ] = array.width * y;
			ro[ 2 ] = array.width * Math.min( y + 1, array.height - 1 );
			for ( int x = 0; x < array.width; ++x )
			{
				// (L(x+1, y) - L(x-1, y)) / 2
				final float der_x = ( array.data[ ro[ 1 ] + Math.min( x + 1, array.width - 1 ) ] - array.data[ ro[ 1 ] + Math.max( 0, x - 1 ) ] ) / 2;

				// (L(x, y+1) - L(x, y-1)) / 2
				final float der_y = ( array.data[ ro[ 2 ] + x ] - array.data[ ro[ 0 ] + x ] ) / 2;

				// amplitude
				gradients[ 0 ].data[ ro[ 1 ] + x ] = ( float ) Math.sqrt( Math.pow( der_x, 2 ) + Math.pow( der_y, 2 ) );
				// orientation
				gradients[ 1 ].data[ ro[ 1 ] + x ] = ( float ) Math.atan2( der_y, der_x );
			}
		}
		return gradients;
	}
    
    /**
	 * In place enhance all values of a FloatArray to fill the given range.
	 * 
	 * @param src
	 *            source
	 * @param scale
	 *            defines the range
	 */
    final static public void enhance( final FloatArray2D src, final float scale )
    {
    	float min = src.data[ 0 ];
    	float max = min;
    	for ( float f : src.data )
    	{
    		if ( f < min ) min = f;
    		else if ( f > max ) max = f;
    	}
    	final float s = scale / ( max - min );
    	for ( int i = 0; i < src.data.length; ++i )
    		src.data[ i ] = s * ( src.data[ i ] - min );
    }
    
    /**
	 * Convolve an image with a horizontal and a vertical kernel
	 * simple straightforward, not optimized---replace this with a trusted better version soon
	 * 
	 * @param input the input image
	 * @param h horizontal kernel
	 * @param v vertical kernel
	 * 
	 * @return convolved image
	 */
	final static public FloatArray2D convolveSeparable(
			final FloatArray2D input,
			final float[] h,
			final float[] v )
	{
		FloatArray2D output = new FloatArray2D( input.width, input.height );
		FloatArray2D temp = new FloatArray2D( input.width, input.height );

		final int hl = h.length / 2;
		final int vl = v.length / 2;
		
		int xl = input.width - h.length + 1;
		int yl = input.height - v.length + 1;
		
		// create lookup tables for coordinates outside the image range
		final int[] xb = new int[ h.length + hl - 1 ];
		final int[] xa = new int[ h.length + hl - 1 ];
		for ( int i = 0; i < xb.length; ++i )
		{
			xb[ i ] = flipInRange( i - hl, input.width );
			xa[ i ] = flipInRange( i + xl, input.width );
		}
		
		final int[] yb = new int[ v.length + vl - 1 ];
		final int[] ya = new int[ v.length + vl - 1 ];
		for ( int i = 0; i < yb.length; ++i )
		{
			yb[ i ] = input.width * flipInRange( i - vl, input.height );
			ya[ i ] = input.width * flipInRange( i + yl, input.height );
		}
		
		xl += hl;
		yl += vl;
		
		// horizontal convolution per row
		final int rl = input.height * input.width;
		for ( int r = 0; r < rl; r += input.width )
		{
			for ( int x = hl; x < xl; ++x )
			{
				final int c = x - hl;
				float val = 0;
				for ( int xk = 0; xk < h.length; ++xk )
				{
					val += h[ xk ] * input.data[ r + c + xk ];
				}
				temp.data[ r + x ] = val;
			}
			for ( int x = 0; x < hl; ++x )
			{
				float valb = 0;
				float vala = 0;
				for ( int xk = 0; xk < h.length; ++xk )
				{
					valb += h[ xk ] * input.data[ r + xb[ x + xk ] ];
					vala += h[ xk ] * input.data[ r + xa[ x + xk ] ];
				}
				temp.data[ r + x ] = valb;
				temp.data[ r + x + xl ] = vala;
			}
		}

		// vertical convolution per column
		final int rm = yl * temp.width;
		final int vlc = vl * temp.width;
		for ( int x = 0; x < temp.width; ++x )
		{
			for ( int r = vlc; r < rm; r += temp.width )
			{
				float val = 0;
				final int c = r - vlc;
				int rk = 0;
				for ( int yk = 0; yk < v.length; ++yk )
				{
					val += v[ yk ] * temp.data[ c + rk + x ];
					rk += temp.width;
				}
				output.data[ r + x ] = val;
			}
			for ( int y = 0; y < vl; ++y )
			{
				final int r = y * temp.width;
				float valb = 0;
				float vala = 0;
				for ( int yk = 0; yk < v.length; ++yk )
				{
					valb += h[ yk ] * temp.data[ yb[ y + yk ] + x ];
					vala += h[ yk ] * temp.data[ ya[ y + yk ] + x ];
				}
				output.data[ r + x ] = valb;
				output.data[ r + rm + x ] = vala;
			}
		}

		return output;
	}
}
