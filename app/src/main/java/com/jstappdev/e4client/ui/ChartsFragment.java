package com.jstappdev.e4client.ui;

import android.os.Bundle;
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

import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SessionData;
import com.jstappdev.e4client.SharedViewModel;
import com.scichart.charting.Direction2D;
import com.scichart.charting.model.AnnotationCollection;
import com.scichart.charting.model.dataSeries.XyDataSeries;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.annotations.AnnotationCoordinateMode;
import com.scichart.charting.visuals.annotations.AxisMarkerAnnotation;
import com.scichart.charting.visuals.annotations.VerticalLineAnnotation;
import com.scichart.charting.visuals.axes.AutoRange;
import com.scichart.charting.visuals.axes.IAxis;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.charting.visuals.synchronization.SciChartVerticalGroup;
import com.scichart.data.model.DoubleRange;
import com.scichart.drawing.utility.ColorUtil;
import com.scichart.extensions.builders.SciChartBuilder;

import java.util.Collections;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChartsFragment extends Fragment {

    private final SciChartVerticalGroup verticalGroup = new SciChartVerticalGroup();
    private final DoubleRange sharedXRange = new DoubleRange();

    @BindView(R.id.edaChart)
    SciChartSurface edaChart;

    @BindView(R.id.hrChart)
    SciChartSurface hrChart;

    @BindView(R.id.tempChart)
    SciChartSurface tempChart;

    @BindView(R.id.bvpChart)
    SciChartSurface bvpChart;

    private SessionData sessionData;
    private SharedViewModel sharedViewModel;
    private SciChartBuilder sciChartBuilder;

    public static final int DEFAULT_POINT_COUNT = 150;
    public static final int SMA_SERIES_COLOR = 0xFFFFA500;
    public static final int STROKE_UP_COLOR = 0xFF00AA00;
    public static final int STROKE_DOWN_COLOR = 0xFFFF0000;

    private static float averageHr = -1.0f;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_charts, container, false);

        ButterKnife.bind(this, root);

        SciChartBuilder.init(getActivity());
        sciChartBuilder = SciChartBuilder.instance();

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


    private void setupChart(SciChartSurface chartSurface, final String yAxisTitle, XyDataSeries<Double, Float> lineData, boolean isFirstPane) {
        AnnotationCollection annotations = new AnnotationCollection();

        // Create a numeric X axis
        final IAxis xAxis = sciChartBuilder.newNumericAxis()
                .withVisibleRange(sharedXRange)
                .withDrawMinorGridLines(false)
                .withGrowBy(0, 0.1)
                .withVisibility(isFirstPane ? View.VISIBLE : View.GONE)
                .build();

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
        sessionData = SessionData.getInstance();
        final LifecycleOwner owner = getViewLifecycleOwner();

        // Create a couple of DataSeries for numeric data
        final XyDataSeries<Double, Float> edaLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        final XyDataSeries<Double, Float> hrLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        final XyDataSeries<Double, Float> bvpLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        //final XyDataSeries<Double, Float> ibiLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        final XyDataSeries<Double, Float> tempLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();

        final AxisMarkerAnnotation hrAxisMarker = sciChartBuilder.newAxisMarkerAnnotation()
                .withY1(0d).withBackgroundColor(SMA_SERIES_COLOR).build();
        final AxisMarkerAnnotation tempAxisMarker = sciChartBuilder.newAxisMarkerAnnotation()
                .withY1(0d).withBackgroundColor(SMA_SERIES_COLOR).build();

        setupChart(edaChart, "EDA uS", edaLineData, true);
        setupChart(hrChart, "HR", hrLineData, false);
        setupChart(bvpChart, "BVP", bvpLineData, false);
        //setupChart(ibiChart, "IBI", ibiLineData);
        setupChart(tempChart, "Temp", tempLineData, false);

        // axis marker showing current value
        Collections.addAll(hrChart.getAnnotations(), hrAxisMarker);
        Collections.addAll(tempChart.getAnnotations(), tempAxisMarker);

        // modifiers for main chart
        // todo: enable zoom and pan when not displaying live data
        if (false)
            Collections.addAll(edaChart.getChartModifiers(), sciChartBuilder.newModifierGroup()
                    .withXAxisDragModifier().build()
                    .withZoomPanModifier().withReceiveHandledEvents(true).withXyDirection(Direction2D.XDirection).build()
                    .withZoomExtentsModifier().build()
                    .build());

        sharedViewModel.getLastGsr().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastGsr) {
                edaLineData.append(sessionData.getGsrTimestamps().getLast(), sessionData.getGsr().get(lastGsr));
                edaChart.zoomExtents();
            }
        });
        sharedViewModel.getLastBvp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastBvp) {
                bvpLineData.append(sessionData.getBvpTimestamps().getLast(), sessionData.getBvp().get(lastBvp));
                bvpChart.zoomExtents();
            }
        });
        sharedViewModel.getLastTemp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastTemp) {
                tempLineData.append(sessionData.getTempTimestamps().getLast(), sessionData.getTemp().get(lastTemp));
                tempAxisMarker.setY1(sessionData.getTemp().get(lastTemp));
                tempChart.zoomExtents();
            }
        });

        sharedViewModel.getLastIbi().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastIbi) {
                final float currentHr = 60.0f / sessionData.getIbi().getLast();

                // heart rate may theoretically reach 600, but we assume 300 max
                // https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3273956/
                if (averageHr != 0.0f && currentHr < 300f) {
                    averageHr = 0.8f * averageHr + 0.2f * currentHr;
                }

                //ibiLineData.append(sessionData.getIbiTimestamps().getLast(), sessionData.getIbi().get(lastBvp));
                hrLineData.append(sessionData.getIbiTimestamps().getLast(), currentHr);
                hrAxisMarker.setY1(averageHr);
                hrChart.zoomExtents();
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


        verticalGroup.addSurfaceToGroup(edaChart);
        verticalGroup.addSurfaceToGroup(bvpChart);
        verticalGroup.addSurfaceToGroup(hrChart);
        verticalGroup.addSurfaceToGroup(tempChart);

    }

}