/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.dsp.fft;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for selecting the optimal implementation of the complex FFT for this hardware, as determined by the
 * Calibration Manager.
 */
public class ComplexFFTFactory
{
    /**
     * Creates the optimal complex FFT implementation.
     * @param size of the transform in complex samples, with no prime factors other than 2, 3 and 5
     * @return complex FFT
     * @throws IllegalArgumentException if the size is not supported
     */
    public static ComplexFFT getComplexFFT(int size)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.FFT_COMPLEX);
        return getComplexFFT(implementation, size);
    }

    /**
     * Creates the complex FFT implementation.
     *
     * Calibration creates its candidates with this method so that it measures exactly what the application uses.
     * @param implementation to create, where UNCALIBRATED creates the scalar implementation, as does the vector
     * implementation on a CPU that it does not support (see VectorComplexFFT.isVectorSupported())
     * @param size of the transform in complex samples, with no prime factors other than 2, 3 and 5
     * @throws IllegalArgumentException if there is no such implementation or the size is not supported
     */
    public static ComplexFFT getComplexFFT(Implementation implementation, int size)
    {
        return switch(implementation)
        {
            case SCALAR, UNCALIBRATED -> getScalarFFT(size);
            case VECTOR_SIMD_PREFERRED -> VectorComplexFFT.isVectorSupported() ? new VectorComplexFFT(size) :
                getScalarFFT(size);
            default -> throw new IllegalArgumentException("No complex FFT implementation for " + implementation);
        };
    }

    /**
     * Creates the scalar FFT: JTransforms for a power of two size and the mixed radix FFT for any other size.
     */
    private static ComplexFFT getScalarFFT(int size)
    {
        return size > 1 && Integer.bitCount(size) == 1 ? new JTransformsComplexFFT(size) :
            new MixedRadixComplexFFT(size);
    }
}
