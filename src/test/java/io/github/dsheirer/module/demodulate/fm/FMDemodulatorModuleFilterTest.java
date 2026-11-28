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
package io.github.dsheirer.module.demodulate.fm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.dsp.filter.ChannelFilterDesigner;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import org.junit.jupiter.api.Test;

/**
 * The FM demodulator's channel filter is now designed by ChannelFilterDesigner at the sample rate it's applied at,
 * with 5000/6250 Hz edges, in place of a remez design at twice the sample rate with 10000/12500 Hz edges and no check
 * of the result.  Over the rates the demodulator filters at (it decimates to a rate from 25 kHz to just under 50 kHz),
 * here in 25 Hz steps:
 * <ul>
 * <li>where the earlier design has a working response (ChannelFilterDesigner.isValidFilterResponse()), the filter is
 * equivalent: identical taps at 25 and 25.6 kHz (the rates behind 50 and 51.2 kHz polyphase channels), and elsewhere
 * the same length with a frequency response within remez round-off (5E-4, -66 dB) at every frequency;</li>
 * <li>where it doesn't (the remez design converged on a filter with a stop band barely down, or above 0 dB), the filter
 * now has a working response.</li>
 * </ul>
 */
public class FMDemodulatorModuleFilterTest
{
    @Test
    void sameFilterWhereTheEarlierDesignWorkedAndAWorkingOneElsewhere() throws Exception
    {
        double bandwidth = FMDemodulatorModule.DEFAULT_CHANNEL_BANDWIDTH;
        int passBandStop = (int)(bandwidth * .4);
        int stopBandStart = (int)(bandwidth * .5);
        int fixed = 0;

        for(double rate = 25000.0; rate < 50000.0; rate += 25.0)
        {
            float[] earlier = earlierDesign(rate, bandwidth);
            assertNotNull(earlier, "earlier design at " + rate);
            float[] taps = FMDemodulatorModule.designChannelFilter(rate, bandwidth);
            assertTrue(ChannelFilterDesigner.isValidFilterResponse(taps, rate, passBandStop, stopBandStart),
                "response at " + rate);

            if(!ChannelFilterDesigner.isValidFilterResponse(earlier, rate, passBandStop, stopBandStart))
            {
                fixed++;
            }
            else if(rate == 25000.0 || rate == 25600.0)
            {
                assertArrayEquals(earlier, taps, "taps at " + rate);
            }
            else
            {
                assertEquals(earlier.length, taps.length, "length at " + rate);

                for(double frequency = 0.0; frequency <= rate / 2.0; frequency += rate / 1024.0)
                {
                    assertEquals(response(earlier, frequency, rate), response(taps, frequency, rate), 5E-4,
                        "response at " + frequency + " Hz at " + rate);
                }
            }
        }

        assertTrue(fixed > 0, "the earlier design failed at some rates");
    }

    private static float[] earlierDesign(double rate, double bandwidth) throws Exception
    {
        return FilterFactory.getTaps(FIRFilterSpecification.lowPassBuilder()
            .sampleRate(rate * 2)
            .gridDensity(16)
            .oddLength(true)
            .passBandCutoff((int)(bandwidth * .8))
            .passBandAmplitude(1.0)
            .passBandRipple(0.01)
            .stopBandStart((int)bandwidth)
            .stopBandAmplitude(0.0)
            .stopBandRipple(0.005)
            .build());
    }

    /**
     * Magnitude response of FIR taps at the frequency.
     */
    private static double response(float[] taps, double frequency, double sampleRate)
    {
        double re = 0, im = 0;

        for(int k = 0; k < taps.length; k++)
        {
            re += taps[k] * Math.cos(2.0 * Math.PI * frequency * k / sampleRate);
            im -= taps[k] * Math.sin(2.0 * Math.PI * frequency * k / sampleRate);
        }

        return Math.hypot(re, im);
    }
}
