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
package io.github.dsheirer.module.decode.nxdn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.dsp.filter.ChannelFilterDesigner;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import org.junit.jupiter.api.Test;

/**
 * The NXDN channel filter (ChannelFilterDesigner.designRemezLowPass()) is a working low-pass at every rate that the
 * decoder filters at (4 to 10 samples per symbol after decimation): pass band within 1 dB and stop band at least 30 dB
 * down, through to half the sample rate.  Where the remez design works, the filter is the remez design.
 */
public class NXDNChannelFilterTest
{
    private static final int CHECK_POINTS = 1024;

    @Test
    void workingFilterAtEveryRate()
    {
        //25 Hz steps for 4800 (9.6 to 24 kHz) and 50 Hz steps for 9600 (19.2 to 48 kHz), plus rates where the remez
        //design of the 4800 edges doesn't converge
        check(TransmissionMode.M4800, 25.0, new double[]{10350.0, 11400.0, 11900.0, 22950.0, 23575.0});
        check(TransmissionMode.M9600, 50.0, new double[0]);
    }

    private static void check(TransmissionMode mode, double step, double[] extraRates)
    {
        DecodeConfigNXDN config = new DecodeConfigNXDN();
        config.setTransmissionMode(mode);
        ChannelSpecification specification = config.getChannelSpecification();
        double pass = specification.getPassFrequency();
        double stop = specification.getStopFrequency();
        int symbolRate = mode.getSymbolRate();

        for(double rate = symbolRate * 4.0 + step; rate < symbolRate * 10.0; rate += step)
        {
            checkRate(specification, rate, pass, stop);
        }

        for(double rate : extraRates)
        {
            checkRate(specification, rate, pass, stop);
        }
    }

    private static void checkRate(ChannelSpecification specification, double rate, double pass, double stop)
    {
        float[] taps = specification.getChannelFilter().getTaps(rate);
        String label = specification.getPassFrequency() + "/" + specification.getStopFrequency() + " at " + rate;
        assertTrue(isWorking(taps, rate, pass, stop), "response " + label);

        float[] remez = FilterFactory.getRemezLowPass((float)rate, pass, stop);

        if(isWorking(remez, rate, pass, stop))
        {
            assertArrayEquals(remez, taps, "remez design " + label);
        }
    }

    private static boolean isWorking(float[] taps, double rate, double pass, double stop)
    {
        double dc = ChannelFilterDesigner.getResponseDb(taps, 0.0, rate);

        for(int x = 0; x <= CHECK_POINTS; x++)
        {
            double frequency = rate / 2.0 * x / CHECK_POINTS;
            double response = ChannelFilterDesigner.getResponseDb(taps, frequency, rate) - dc;

            if((frequency <= pass && response > 1.0) || (frequency >= stop && response > -30.0))
            {
                return false;
            }
        }

        return true;
    }
}
