package com.jstappdev.e4client.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.Utils;
import com.scichart.charting.Direction2D;
import com.scichart.charting.model.dataSeries.XyDataSeries;
import com.scichart.charting.numerics.labelProviders.DateLabelProvider;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.annotations.AxisMarkerAnnotation;
import com.scichart.charting.visuals.annotations.VerticalLineAnnotation;
import com.scichart.charting.visuals.axes.AutoRange;
import com.scichart.charting.visuals.axes.IAxis;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.charting.visuals.synchronization.SciChartVerticalGroup;
import com.scichart.data.model.DateRange;
import com.scichart.data.model.DoubleRange;
import com.scichart.drawing.utility.ColorUtil;
import com.scichart.extensions.builders.SciChartBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChartsFragment extends Fragment {

    private final SciChartVerticalGroup verticalGroup = new SciChartVerticalGroup();
    private final DateRange sharedXRange = new DateRange();

    @BindView(R.id.edaChart)
    SciChartSurface edaChart;

    @BindView(R.id.hrChart)
    SciChartSurface hrChart;

    @BindView(R.id.tempChart)
    SciChartSurface tempChart;

    // @BindView(R.id.bvpChart)
    //SciChartSurface bvpChart;

    private SharedViewModel sharedViewModel;
    private SciChartBuilder sciChartBuilder;

    private static final int AXIS_MARKER_COLOR = 0xFFFFA500;
    private static final int HRV_MARKER_COLOR = 0x00FFA500;

    private static float averageHr = -1.0f;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(requireActivity())).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_charts, container, false);

        ButterKnife.bind(this, root);

        SciChartBuilder.init(requireActivity());
        sciChartBuilder = SciChartBuilder.instance();

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupCharts();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SciChartBuilder.dispose();
    }

    public static class DateLabelProviderEx extends DateLabelProvider {
        @Override
        public String formatLabel(Comparable dataValue) {
            // return a formatting string for tick labels
            return Utils.getDuration(Math.round((Double) dataValue));
        }
    }

    private void setupChart(SciChartSurface chartSurface, final String yAxisTitle, XyDataSeries<Double, Float> lineData, boolean isFirstPane) {

        final IAxis xAxis = sciChartBuilder.newDateAxis()
                .withVisibleRange(sharedXRange)
                .withDrawMinorGridLines(false)
                .withGrowBy(0, 0.1)
                .withVisibility(isFirstPane ? View.VISIBLE : View.GONE)
                .build();
        xAxis.setLabelProvider(new DateLabelProviderEx());

        // Create a numeric Y axis
        final IAxis yAxis = sciChartBuilder.newNumericAxis()
                .withTextFormatting("0.0")
                .withAutoRangeMode(AutoRange.Always)
                .withAxisTitle(yAxisTitle)
                .withDrawMinorGridLines(true)
                .withDrawMajorGridLines(true)
                .withMinorsPerMajor(isFirstPane ? 4 : 2)
                .withMaxAutoTicks(isFirstPane ? 8 : 4)
                .withGrowBy(new DoubleRange(0d, 0d))
                .build();

        // Add the Y axis to the YAxes collection of the surface
        Collections.addAll(chartSurface.getYAxes(), yAxis);
        // Add the X axis to the XAxes collection of the surface
        Collections.addAll(chartSurface.getXAxes(), xAxis);

        // Create and configure a line series
        final IRenderableSeries lineSeries = sciChartBuilder.newLineSeries()
                .withDataSeries(lineData)
                .withStrokeStyle(ColorUtil.LightBlue, 2f, true)
                .build();

        chartSurface.getRenderableSeries().add(lineSeries);
    }

    private void setupCharts() {
        final LifecycleOwner owner = getViewLifecycleOwner();

        // Create a couple of DataSeries for numeric data
        final XyDataSeries<Double, Float> edaLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        final XyDataSeries<Double, Float> hrLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        // final XyDataSeries<Double, Float> bvpLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        final XyDataSeries<Double, Float> tempLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        // final XyDataSeries<Double, Float> ibiLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();

        final AxisMarkerAnnotation hrAxisMarker = sciChartBuilder.newAxisMarkerAnnotation()
                .withY1(0d).withBackgroundColor(AXIS_MARKER_COLOR).build();
        final AxisMarkerAnnotation hrvAxisMarker = sciChartBuilder.newAxisMarkerAnnotation()
                .withY1(0d).withBackgroundColor(HRV_MARKER_COLOR).build();
        final AxisMarkerAnnotation tempAxisMarker = sciChartBuilder.newAxisMarkerAnnotation()
                .withY1(0d).withBackgroundColor(AXIS_MARKER_COLOR).build();

        setupChart(edaChart, "EDA uS", edaLineData, true);
        setupChart(hrChart, "HR", hrLineData, false);
        // setupChart(bvpChart, "BVP", bvpLineData, false);
        setupChart(tempChart, "Temp", tempLineData, false);
        //setupChart(ibiChart, "IBI", ibiLineData);

        // axis marker showing current value
        Collections.addAll(hrChart.getAnnotations(), hrAxisMarker, hrvAxisMarker);
        Collections.addAll(tempChart.getAnnotations(), tempAxisMarker);

        verticalGroup.addSurfaceToGroup(edaChart);
        verticalGroup.addSurfaceToGroup(hrChart);
        verticalGroup.addSurfaceToGroup(tempChart);
        //verticalGroup.addSurfaceToGroup(bvpChart);


        // modifiers for main chart
        if (!sharedViewModel.getIsConnected().getValue()) {

            sharedXRange.setMin(new Date(sharedViewModel.getSessionData().getInitialTime()));

            Collections.addAll(edaChart.getChartModifiers(), sciChartBuilder.newModifierGroup()
                    .withXAxisDragModifier().build()
                    .withZoomPanModifier().withReceiveHandledEvents(true).withXyDirection(Direction2D.XDirection).build()
                    .withZoomExtentsModifier().build()
                    .build());

            edaLineData.append(sharedViewModel.getSessionData().getGsrTimestamps(), sharedViewModel.getSessionData().getGsr());
            hrLineData.append(sharedViewModel.getSessionData().getHrTimestamps(), sharedViewModel.getSessionData().getHr());
            tempLineData.append(sharedViewModel.getSessionData().getTempTimestamps(), sharedViewModel.getSessionData().getTemp());
            //   bvpLineData.append(sharedViewModel.getSesssionData().getBvpTimestamps(), sharedViewModel.getSesssionData().getBvp());

            for (double tag : sharedViewModel.getSessionData().getTags()) {
                Log.d(MainActivity.TAG, "added tag " + tag);

                VerticalLineAnnotation verticalLine = sciChartBuilder.newVerticalLineAnnotation()
                        .withPosition(tag, 0.5d)
                        .withStroke(2, ColorUtil.Orange)
                        .withVerticalGravity(Gravity.FILL_VERTICAL)
                        .withIsEditable(false)
                        .build();

                Collections.addAll(edaChart.getAnnotations(), verticalLine);
            }

            // fixme: annotation not visible
            // display heart rate variability
            final double hrv = Utils.calcHrvSDNN(sharedViewModel.getSessionData().getHr());
            Log.d(MainActivity.TAG, "calulated HRV: " + hrv);
            hrvAxisMarker.setY1(hrv);

            edaChart.animateZoomExtents(500);

            return;
        }


        sharedViewModel.getLastGsr().observe(owner, new Observer<Integer>() {
            private int x = 0;

            @Override
            public void onChanged(Integer lastGsr) {
                if (x++ % 100 == 0) {
                    edaLineData.append(sharedViewModel.getSessionData().getGsrTimestamps().getLast(), sharedViewModel.getSessionData().getGsr().get(lastGsr));
                    edaChart.zoomExtents();
                }
            }
        });
        /*
        sharedViewModel.getLastBvp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastBvp) {
                bvpLineData.append(sharedViewModel.getSessionData().getBvpTimestamps().getLast(), sharedViewModel.getSesssionData().getBvp().get(lastBvp));
                bvpChart.zoomExtents();
            }
        });
         */
        sharedViewModel.getLastTemp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastTemp) {
                tempLineData.append(sharedViewModel.getSessionData().getTempTimestamps().getLast(), sharedViewModel.getSessionData().getTemp().get(lastTemp));
                tempAxisMarker.setY1(sharedViewModel.getSessionData().getTemp().get(lastTemp));
                tempChart.zoomExtents();
            }
        });

        sharedViewModel.getLastIbi().observe(owner, new Observer<Integer>() {
            private int x = 0;

            @Override
            public void onChanged(Integer lastIbi) {
                final float currentHr = 60.0f / sharedViewModel.getSessionData().getIbi().getLast();

                // heart rate may theoretically reach 600, but we assume 300 max
                // https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3273956/
                if (averageHr != 0.0f && currentHr < 300f) {
                    averageHr = 0.8f * averageHr + 0.2f * currentHr;
                }

                if (x++ % 100 == 0) {
                    hrLineData.append(sharedViewModel.getSessionData().getIbiTimestamps().getLast(), currentHr);
                    hrAxisMarker.setY1(averageHr);
                    hrChart.zoomExtents();
                }
            }
        });

        sharedViewModel.getTag().observe(owner, new Observer<Double>() {
            @Override
            public void onChanged(Double tag) {
                VerticalLineAnnotation verticalLine = sciChartBuilder.newVerticalLineAnnotation()
                        .withPosition(tag, 0.5d)
                        .withStroke(2, ColorUtil.Orange)
                        .withVerticalGravity(Gravity.FILL_VERTICAL)
                        .withIsEditable(false)
                        .build();

                Collections.addAll(edaChart.getAnnotations(), verticalLine);
            }
        });


    }

}