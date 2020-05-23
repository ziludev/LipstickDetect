//
// Created by 王福发 on 2020/4/21.
//

#ifndef LIPSTICKDETECT_STD_ROUND_H
#define LIPSTICKDETECT_STD_ROUND_H

#include <sstream>
#include "../std_to_string.h"

using namespace std;

namespace std {
    template <typename T> T round(T v) {
        return (v > 0) ? (v + 0.5) : (v - 0.5);
    }
}

#endif //LIPSTICKDETECT_STD_ROUND_H
