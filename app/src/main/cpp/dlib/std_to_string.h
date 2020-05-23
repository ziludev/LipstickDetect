//
// Created by 王福发 on 2020/4/21.
//

#ifndef LIPSTICKDETECT_STD_TO_STRING_H
#define LIPSTICKDETECT_STD_TO_STRING_H

#include <string>
#include <sstream>
#include "../../../../../../../Library/Android/sdk/ndk/android-ndk-r16b/sources/cxx-stl/gnu-libstdc++/4.9/include/sstream"
#include "../../../../../../../Library/Android/sdk/ndk/android-ndk-r16b/sources/cxx-stl/gnu-libstdc++/4.9/include/string"

using namespace std;
namespace std
{
    template < typename T > std::string to_string( const T& n )
    {
        std::ostringstream stm ;
        stm << n ;
        return stm.str() ;
    }
}

#endif //LIPSTICKDETECT_STD_TO_STRING_H
