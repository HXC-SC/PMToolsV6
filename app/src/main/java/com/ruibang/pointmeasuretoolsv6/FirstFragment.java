package com.ruibang.pointmeasuretoolsv6;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.ruibang.pointmeasuretoolsv6.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // 使用数据绑定库来创建视图
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 确保按钮和导航配置正确
        if (binding != null && binding.buttonFirst != null) {
            binding.buttonFirst.setOnClickListener(v ->
                    NavHostFragment.findNavController(FirstFragment.this)
                            .navigate(R.id.action_FirstFragment_to_SecondFragment)
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清除绑定，防止内存泄漏
        binding = null;
    }
}